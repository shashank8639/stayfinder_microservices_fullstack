-- One MySQL process, three logical databases.
-- Interview: this is a demo of database-per-service, not three MySQL servers.
CREATE DATABASE IF NOT EXISTS stayfinder_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS stayfinder_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS stayfinder_food CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
