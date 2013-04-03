Database Migrations
===================

0.4.7 -> 0.4.8
==============

Mistakes were made in the migrations from 0.4.6 to 0.4.7 which necessitated rewriting the migrations so we could
migrate 0.4.6 installations to 0.4.8. Since the tables didn't change we just need to update the checksums so
we can run resetdb in 0.4.8. Run the following sql commands using "psql reef_d". You can find your databaseName
in property: org.totalgrid.reef.sql.database. If you don't care if you lose your data just run "resetdb --hard"
and that will mean you wont need to run this step.

```
update databasechangelog set "md5sum" = '3:ce90a00dee488fc6f14c34f8a69ba549' where "id" = '1331138724917-1';
update databasechangelog set "md5sum" = '3:9090508e4ff823633e2ec6d3f9aaf5f1' where "id" = '1331673009469-1';
update databasechangelog set "md5sum" = '3:9d63a4b1bd08a1ae32a8235f67dc0024' where "id" = '1331837983992-1';
update databasechangelog set "md5sum" = '3:d7900723ba095ed035247807a472c8d0' where "id" = '1332380500619-1';
update databasechangelog set "md5sum" = '3:96ab2863ff9ba9d689d0208009cbbf9d' where "id" = '1332380500619-3';
update databasechangelog set "md5sum" = '3:9e65b038d5b8bb0411fb11879f2ba3f4' where "id" = '1332519044036-1';
update databasechangelog set "md5sum" = '3:1a4be13f3c911d0545613afec3fd196a' where "id" = '1332519044036-2';
update databasechangelog set "md5sum" = '3:466ebb30c2d30342b5f2b8ee329cbf0a' where "id" = '1333636303890-1';
```

You will know you need to run this step if you get this message:

    Reset failed: liquibase.exception.ValidationFailedException: Validation Failed:

0.4.6 -> 0.4.7
==============

This migration is impossible to run, but it is possible to skip to 0.4.8 and migrate straight from 0.4.6 -> 0.4.8.

You will however lose the following information:

* Custom Permissions - the permission system saw a major overhaul and it was not possible to migrate old definitions
* Command History - Command Histories are now tracked with their locks so we can trace a command back to the issuing
  agent. This means older histories will be removed.
* Applications - All applications registrations will be removed but that is generally not a problem

0.4.6
==============

First version with migration support, obviously there are no previous migrations possible.