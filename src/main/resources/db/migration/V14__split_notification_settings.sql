ALTER TABLE users
    ADD COLUMN notify_group_remind  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN notify_todo_deadline BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN notify_group_invite  BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    DROP COLUMN notify_agree;
