### Allure Reporter

Used to save the report in Allure Report format

```json
{
  "Main": {
    "reporterImpl": "reporter.allure.AllureReporter"
  },
  "AllureReporter": {
    "reportDir": "Path to the directory to save the report (optional)"
  }
}
```

If reportDir is not specified, the default path will be used (<module_name>/build/allure-results/).

### CSV Reporter

Used to save the report in CSV format

```json
{
  "Main": {
    "reporterImpl": "reporter.csv.CsvReporter"
  },
  "CsvReporter": {
    "reportDir": "Path to the directory to save the report (optional)"
  }
}
```

If reportDir is not specified, the default path will be used (<module_name>/build/csv-results/).

### Database Reporter

Used to save the report to the database

```json
{
  "Main": {
    "reporterImpl": "reporter.database.DatabaseReporter"
  },
  "DatabaseReporter": {
    "driver": "org.postgresql.Driver",
    "url": "jdbc:postgresql://localhost:5432/db_name",
    "user": "user",
    "password": "password",
    "projectId": "3f1a4c2e-5b6d-4a8f-9c0d-1e2f3a4b5c6d"
  }
}
```

- `projectId` — UUID of a project created in the report application. The runner writes runs, tests and steps into that project.
- The project must already exist: the runner does not create projects automatically. A missing project aborts the run with an error.
- `projectId` is mandatory. If it is absent or is not a valid UUID, the runner fails on start with a configuration error.
- Schema creation by the runner is no longer supported: the database schema is created by the report application on demand, and the runner never creates or alters it.

### Several at once

Saving a report in several formats at once

```json
{
  "Main": {
    "reporterImpl": [
      "reporter.allure.AllureReporter",
      "reporter.database.DatabaseReporter"
    ]
  },
  "DatabaseReporter": {
    "driver": "org.postgresql.Driver",
    "url": "jdbc:postgresql://localhost:5432/db_name",
    "user": "user",
    "password": "password",
    "projectId": "3f1a4c2e-5b6d-4a8f-9c0d-1e2f3a4b5c6d"
  }
}
```

- `projectId` — UUID of a project created in the report application. The runner writes runs, tests and steps into that project.
- The project must already exist: the runner does not create projects automatically. A missing project aborts the run with an error.
- `projectId` is mandatory. If it is absent or is not a valid UUID, the runner fails on start with a configuration error.
- Schema creation by the runner is no longer supported: the database schema is created by the report application on demand, and the runner never creates or alters it.
