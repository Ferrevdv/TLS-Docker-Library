import argparse
import json
from concurrent.futures import ThreadPoolExecutor, as_completed
import subprocess
from subprocess import STDOUT, PIPE
import datetime
import sys
import os
import math
import threading

class DockerImage:
    """
    Helper class which holds details about a docker Image. Can be built and pushed
    """
    def __init__(self, dockerfile, version, instances, image_version, build_args, docker_repo, library_name, latest: bool, counter: int):
        self.dockerfile = dockerfile
        self.version = version
        self.instance = instances
        self.image_version = image_version
        self.build_args = build_args
        self.docker_repo = docker_repo
        self.library_name = library_name
        self.latest = latest
        self.build_command = ''
        self.counter = counter

        # embed version number in image version
        self.image_version = self.image_version.format(v=self.version)

        # construct tag
        self.version_tag = '{docker_repo}{name}-{instance}:{image_version}'.format(docker_repo=self.docker_repo, name=self.library_name, instance=self.instance, image_version=self.image_version)
    
    def exists(self):
        return not self.exec_docker_command("docker images -q " +  self.version_tag).stdout.strip == ""

    def build(self):        
        # construct string from build args
        build_args_str = ''
        for build_arg, build_arg_value in self.build_args.items():
            # replace any {v} in the build args with the current version
            build_arg_value = build_arg_value.format(v=self.version)
            build_args_str += '--build-arg {}={}'.format(build_arg, build_arg_value)# optionally tag as latest
        if self.latest:
            tags =  self.version_tag + ' -t {docker_repo}{name}-{instance}:latest'.format(docker_repo=self.docker_repo, name=self.library_name, instance=self.instance)
        else:
            tags = self.version_tag
        self.build_command = 'docker build {build_args} -t {tags} -f {dockerfile} --target {name}-{instance} --no-cache .'.format(build_args=build_args_str, tags=tags, dockerfile=self.dockerfile, name=self.library_name, instance=self.instance)
        return self.exec_docker_command(self.build_command).returncode
    
    def push(self):
        complete = self.exec_docker_command('docker push --all-tags {}'.format(self.orig_tag)).returncode
        if self.latest:
            complete |= self.exec_docker_command('docker push --all-tags {}'.format('{docker_repo}{name}{instance}:latest'.format(self.docker_repo, self.library_name, self.instance))).returncode
        return complete

    def exec_docker_command(self, command: str):
        return subprocess.run(command.split(' '), stdout=PIPE, stderr=STDOUT, encoding="utf-8")

class LibraryBuilder:

    def __init__(self, build_file, parallel_builds, libraries, force_rebuilds, versions, docker_repo):   
        self.folder = os.path.abspath(os.path.dirname(__file__))
        self.log_succeed = open(os.path.join(self.folder, "build_succeeded.log"), "w")
        self.log_failed = open(os.path.join(self.folder, "build_failed.log"), "w")
        self.log_counter_lock = threading.Lock()
        self.build_file = build_file
        self.parallel_builds = parallel_builds
        self.libraries = libraries
        self.force_rebuilds = force_rebuilds
        self.versions = versions
        self.docker_repo = docker_repo
        self.counter = 0

    def warn(self, log):
        print("{}\033[1;33m [!️] {}\033[0m".format(datetime.datetime.now().isoformat(timespec='seconds'), log))

    def error(self, log):
        print("{}\033[1;31m [-] {}\033[0m".format(datetime.datetime.now().isoformat(timespec='seconds'), log))

    def success(self, log):
        print("{}\033[1;32m [+] {}\033[0m".format(datetime.datetime.now().isoformat(timespec='seconds'), log))

    def info(log):
        print("{}\033[1;34m [i] {}\033[0m".format(datetime.datetime.now().isoformat(timespec='seconds'), log))

    def docker_images_from_build_group(self, build_group: dict, docker_repo: str, library_name: str, latest: str):
        """
        Yields a group of docker images to be built
        """
        dockerfile = build_group['dockerfile']
        versions = build_group['versions']
        instances = build_group['instances']
        image_version = build_group['image_version']
        build_args = build_group['build_args']

        dockerfile = '{}/{}'.format(library_name, dockerfile)


        for version in versions:
            is_latest = latest == image_version.format(v=version)
            for instance in instances:
                self.counter += 1
                yield DockerImage(dockerfile, version, instance, image_version, build_args, docker_repo, library_name, is_latest, self.counter)



    def parse_library(self, library: str):
        images = []

        library_json = '{library}/build.json'.format(library=library)
        # load dict from json
        with open(library_json, 'r') as f:
            json_obj = dict(json.load(f))

        build_groups = json_obj['build_groups']
        # helpful for tagging the last image with :latest as well
        latest_version = json_obj['latest']
        for _, build_group_dict in build_groups.items():
            images += [x for x in self.docker_images_from_build_group(build_group_dict, self.docker_repo, library, latest_version)]
        return images


    def execute_bulk(self, image: DockerImage):
        # skip if existent and we dont want to rebuild
        if image.exists() and not self.force_rebuilds:
            self.warn(("{}/{}, build skipped: {} ").format(image.counter, len(self.futures), image.version_tag))
            return
        # build always
        if image.build() == 0:
            self.success(("{}/{}, build succeeded: {} ").format(image.counter, len(self.futures), image.version_tag))
            # and push if necessary
            if self.docker_repo != '':
                if image.push() == 0:
                    self.success(("{}/{}, push succeeded: {} ").format(image.counter, len(self.futures), image.version_tag))
                else:
                    self.error(("{}/{}, push failed: {} ").format(image.counter, len(self.futures), image.version_tag))
        else:
            self.error(("{}/{}, build failed: {} ").format(image.counter, len(self.futures), image.build_command))


    def build(self):
        with open(self.build_file, 'r') as f:
            libraries = dict(json.load(f))['Libraries']

        # filter libraries to libraries given in ARGS
        if self.libraries != []:
            libraries = list(filter(lambda x: x in self.libraries, libraries))

        # parse all library jsons into docker commands
        images_to_process = []
        for library in libraries:
            images_to_process += self.parse_library(library)

        # put everything into a ThreadPoolExecutor
        self.futures = []
        with ThreadPoolExecutor(self.parallel_builds) as executor:
            for image in images_to_process:
                self.futures.append(executor.submit(self.execute_bulk, image))

        if len(self.futures) == 0:
            self.error("No images found that match your request...")
            sys.exit(1)

        # collect all futures
        for future in as_completed(self.futures):
            pass

def main():
    parser = argparse.ArgumentParser(description="Build docker images for all TLS libraries or for specific ones.")
    parser.add_argument("-p", "--parallel_builds", help="Number of parallel docker build operations", default=os.cpu_count()//2, type=int)
    parser.add_argument("-l", "--library", help="Build only docker images of a certain library. " +
                                                "The value is matched against the subfolder names inside the images folder. " +
                                                "Can be specified multiple times.", default=[], action="append")
    parser.add_argument("-f", "--force_rebuild", help="Build docker containers, even if they already exist.", default=False, action="store_true")
    parser.add_argument("-v", "--version", help="Only build specific versions, this is a regex that is matched against the version. Dots are escaped.", default=[], action="append")
    parser.add_argument("-d", "--deploy", help="Deploy the project to a given repository. Be sure to use docker login and logout yourself", default='', type=str)

    ARGS = parser.parse_args()

    builder = LibraryBuilder('libraries.json', ARGS.parallel_builds, ARGS.library, ARGS.force_rebuild, ARGS.version, ARGS.deploy)
    builder.build()
        
if __name__ == '__main__':
    main()
    print("\nFinished!")