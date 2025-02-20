# Stage 1: Build the ELL from source
FROM debian:latest as ell-base1
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
RUN git clone --depth=1 --branch 0.69 https://git.kernel.org/pub/scm/libs/ell/ell.git /ell-source

# Build ELL from source
WORKDIR /ell-source
RUN ./bootstrap && ./configure --prefix=/usr && make && make install

# Build example server
RUN gcc -o examples/https-server-test examples/https-server-test.c -I/usr/include/ell -L/usr/lib -lell
RUN gcc -o examples/https-client-test examples/https-client-test.c -I/usr/include/ell -L/usr/lib -lell

# Stage 2: Runtime Image
FROM entrypoint as ell-base2
# Copy built ELL from the build stage
COPY --from=ell-base1 /usr /usr
COPY --from=ell-base1 /ell-source /ell-source

# Stage 3: Prepare the server instance
FROM ell-runtime as ell-server
LABEL "tls_implementation"="ell"
LABEL "tls_implementation_version"="${VERSION}"
LABEL "tls_implementation_connectionRole"="server"
COPY --from=ell-base1 /ell-source/examples/https-server-test /bin/
ENTRYPOINT ["server-entrypoint", "https-server-test"]



FROM ell-runtime as ell-client
LABEL "tls_implementation"="ell"
LABEL "tls_implementation_version"="${VERSION}"
LABEL "tls_implementation_connectionRole"="client"
COPY --from=ell-base1 /ell-source/examples/https-client-test /bin/
ENTRYPOINT ["client-entrypoint", "https-client-test"]
