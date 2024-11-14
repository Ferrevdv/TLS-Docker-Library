# Stage 1: Build the ELL from source
FROM debian:latest as ell-build

ARG VERSION
RUN apt-get update && apt-get install -y \
    build-essential \
    gcc \
    libtool \
    autoconf \
    automake \
    pkg-config \
    openssl \
    libssl-dev \
    git \
    && rm -rf /var/lib/apt/lists/*

# Clone the ELL repository at the specified version
RUN git clone --depth=1 --branch=${VERSION} https://github.com/ell/ell.git /ell-source

# Build ELL from source
WORKDIR /ell-source
RUN ./bootstrap && ./configure --prefix=/usr && make && make install

# Stage 2: Runtime Image
FROM debian:latest as ell-runtime

ARG VERSION
LABEL "tls_implementation"="ell"
LABEL "tls_implementation_version"="${VERSION}"
LABEL "tls_implementation_connectionRole"="server"

# Copy built ELL from the build stage
COPY --from=ell-build /usr /usr
COPY --from=ell-build /ell-source /ell-source

# Stage 3: Prepare the server instance
FROM ell-runtime as ell-server

# Example of compiling an ELL-based HTTPS server
WORKDIR /ell-source/examples
RUN gcc -o https-server-test https-server-test.c -I/usr/include/ell -L/usr/lib -lell

# Expose the port the server will listen on
EXPOSE 1234

# Set the entrypoint to run the ELL HTTPS server directly
ENTRYPOINT ["/ell-source/examples/https-server-test", "/certs/rsa2048cert.pem", "/certs/rsa2048key.pem", "", "/certs/ca.pem"]
