FROM rustlang/rust:nightly-buster


WORKDIR /usr/src/myapp
COPY Cargo.toml .
COPY Cargo.lock .
COPY src src

RUN ls
RUN ls src
RUN cat Cargo.toml
RUN cargo install --path .

ENV ROCKET_ADDRESS=0.0.0.0
EXPOSE 8000
CMD ["lab2_hw_rest"]
