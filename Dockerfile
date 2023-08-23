FROM gradle:jdk19

WORKDIR /app

ADD . .

RUN gradle distTar

WORKDIR /app/build/distributions

RUN tar -xf altaria-1.0.0.tar

CMD ./altaria-1.0.0/bin/altaria