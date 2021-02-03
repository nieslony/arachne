/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:  claas
 * Created: 30.04.2017
 */

CREATE TABLE scheduledTasks (
    id serial NOT NULL PRIMARY KEY,
    taskClass text NOT NULL,
    startupDelay bigint,
    interval bigint,
    comment text,
    isEnabled boolean,
    UNIQUE(taskClass, startupDelay, interval)
);;

INSERT INTO scheduledTasks (taskClass, startupDelay, interval, isEnabled) VALUES
    ( 'at.nieslony.openvpnadmin.tasks.UpdateCrl',
        60 * 1,
        60 * 60 * 24 * 7,
        true
    ),
    ( 'at.nieslony.openvpnadmin.tasks.DropDisapprovedConnectedUsers',
        0,
        60 * 2,
        true
    ),
    ( 'at.nieslony.openvpnadmin.tasks.AutoRenewServerfCertificate',
        60,
        60 * 60 * 24,
        true
    ),
    ( 'at.nieslony.openvpnadmin.tasks.CreateDhParams',
        0,
        60 * 60 * 24 * 365,
        false
    )
;;
