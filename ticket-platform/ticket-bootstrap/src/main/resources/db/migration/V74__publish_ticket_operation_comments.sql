-- Publish remarks copied from workflow operations, including existing ones.
UPDATE ticket_comment
SET visibility = 'PUBLIC'
WHERE type = 'OPERATION' AND visibility = 'INTERNAL';
