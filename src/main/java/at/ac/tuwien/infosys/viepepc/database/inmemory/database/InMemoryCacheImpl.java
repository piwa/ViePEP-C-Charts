package at.ac.tuwien.infosys.viepepc.database.inmemory.database;


import at.ac.tuwien.infosys.viepepc.database.entities.virtualmachine.VMType;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
@Getter
public class InMemoryCacheImpl {
    private List<VMType> vmTypeVmMap = new ArrayList<>();

    public void clear() {
        vmTypeVmMap = new ArrayList<>();
    }


    public void addVMType(VMType vmType) {
        vmTypeVmMap.add(vmType);
    }

    public void addAllVMType(List<VMType> vmTypes) {
        for(VMType vmType : vmTypes) {
            addVMType(vmType);
        }
    }

}
