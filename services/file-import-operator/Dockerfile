FROM python:3.12-slim@sha256:cb33f7f771ecec2ef1225cf39b159591a040391bd73e4c01f00fdfc4c47611d2 AS verify
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY *.py ./
COPY tests/ ./tests/
COPY examples/ ./examples/
COPY item-mapping.example.json config.oracle-items.example.json ./
RUN python -m unittest discover -s tests

FROM python:3.12-slim@sha256:cb33f7f771ecec2ef1225cf39b159591a040391bd73e4c01f00fdfc4c47611d2
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY --from=verify /app/*.py ./
USER 65532:65532
EXPOSE 18792 18793
ENTRYPOINT ["python", "bootstrap.py"]
CMD ["--kind", "work-orders", "--config", "/run/import/config.json"]
