package at.ac.tuwien.infosys.viepepc.database.entities;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class VMActionsDTO {

    Date date;
    int amount;
    private String VMAction;
    private String VMID;

    public int getCount() {
        int result = 0;
        if (VMID.startsWith("1_")) result = 2;
        else if (VMID.startsWith("2_")) result = 3;
        else if (VMID.startsWith("3_")) result = 4;
        else if (VMID.startsWith("4_")) result = 5;
        else if (VMID.startsWith("5_")) result = 2;
        else if (VMID.startsWith("6_")) result = 4;
        else if (VMID.startsWith("7_")) result = 8;
        else result = 1;
        result = (!VMAction.equalsIgnoreCase("START")) ? result * -1 : result;
        return result;
    }

}

