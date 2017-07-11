package at.ac.tuwien.infosys.viepepc.database.entities;

import at.ac.tuwien.infosys.viepepc.database.entities.virtualmachine.VMType;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class VMActionsDTO {

    Date date;
    int coreAmount;
    private String VMAction;
    private String VMID;
    private String VMTypeID;


}

