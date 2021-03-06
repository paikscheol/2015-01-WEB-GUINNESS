/* DATETIME COL NAME CHANGE */
ALTER TABLE USERS CHANGE createDate userCreateDate DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE GROUPS CHANGE createDate groupCreateDate DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE NOTES CHANGE createDate noteCreateDate DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE NOTES CHANGE targetDate noteTargetDate DATETIME Not Null;
ALTER TABLE COMMENTS CHANGE createDate commentCreateDate DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE CONFIRMS CHANGE createDate confirmCreateDate DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE ALARMS CHANGE createDate alarmCreateDate DATETIME DEFAULT CURRENT_TIMESTAMP;