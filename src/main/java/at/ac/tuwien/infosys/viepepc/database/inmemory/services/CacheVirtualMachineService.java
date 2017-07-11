package at.ac.tuwien.infosys.viepepc.database.inmemory.services;

import at.ac.tuwien.infosys.viepepc.database.entities.virtualmachine.VMType;
import at.ac.tuwien.infosys.viepepc.database.inmemory.database.InMemoryCacheImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by philippwaibel on 10/06/16. modified by Gerta Sheganaku
 */
@Component
@Slf4j
public class CacheVirtualMachineService {

    @Autowired
    private InMemoryCacheImpl inMemoryCache;


    public List<VMType> getVMTypes() {
        return inMemoryCache.getVmTypeVmMap();
    }

    public VMType getVmTypeFromIdentifier(String identifier) throws Exception {
        for(VMType vmType : getVMTypes()) {
            if(vmType.getIdentifier().toString().equals(identifier)) {
                return vmType;
            }
        }
        throw new Exception("TYPE not found");
    }

    public VMType getVmTypeFromCore(int cores, String location) throws Exception {
        for(VMType vmType : getVMTypes()) {
            if(vmType.getCores() == cores && vmType.getLocation().equals(location)) {
                return vmType;
            }
        }
        throw new Exception("TYPE not found");
    }

}
