/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.tasks;

/**
 *
 * @author claas
 */
@ScheduledTaskInfo(
        description = "Update CRL from database",
        name = "Update CRL"
)
public class UpdateCrl
        implements ScheduledTask
{
    @Override
    public void run() {

    }
}
