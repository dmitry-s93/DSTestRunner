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
    "createSchema": true
  }
}
```

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
    "createSchema": true
  }
}
```