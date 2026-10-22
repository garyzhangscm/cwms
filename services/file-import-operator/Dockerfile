FROM python:3.12-alpine@sha256:b658b05ce9c4eb97ec6fc19424818a1f31993659c2b505a532f470e83807815d AS verify
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY *.py ./
COPY tests/ ./tests/
COPY examples/ ./examples/
COPY item-mapping.example.json config.oracle-items.example.json ./
RUN python -m unittest discover -s tests

FROM python:3.12-alpine@sha256:b658b05ce9c4eb97ec6fc19424818a1f31993659c2b505a532f470e83807815d
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY --from=verify /app/*.py ./
USER 65532:65532
EXPOSE 18792 18793
ENTRYPOINT ["python", "bootstrap.py"]
CMD ["--kind", "work-orders", "--config", "/run/import/config.json"]
