package at.ac.tuwien.infosys.viepepc.database.services;

import at.ac.tuwien.infosys.viepepc.database.entities.VMActionsDTO;
import at.ac.tuwien.infosys.viepepc.database.entities.WorkflowDTO;
import at.ac.tuwien.infosys.viepepc.database.entities.virtualmachine.VMType;
import at.ac.tuwien.infosys.viepepc.database.inmemory.services.CacheVirtualMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by philippwaibel on 24/04/2017.
 */
@Component
public class VMActionsService {

    @Autowired
    private CacheVirtualMachineService cacheVirtualMachineService;

    @Value("${spring.datasource.url}")
    private String databaseUrl = "jdbc:mysql://localhost:3306/";
    @Value("${spring.datasource.url.parameter}")
    private String databaseUrlParameter = "?autoReconnect=true&useSSL=false";
    @Value("${spring.datasource.username}")
    private String databaseUsername = "viepep";
    @Value("${spring.datasource.password}")
    private String databasePassword = "";

    /**
     * @param dbName
     * @param firstDate
     * @param maxDurationInMinutes
     * @param isBaseline           if this is true, than replace the 0_ variable name with 3_, since in baseline only quadcores were allowed
     * @return
     * @throws SQLException
     * @throws ParseException
     */
    public List<VMActionsDTO> getVMActionsDTOs(String dbName, Date firstDate, int maxDurationInMinutes, boolean isBaseline) throws SQLException, ParseException {

        Connection conn = DriverManager.getConnection(databaseUrl.concat(dbName).concat(databaseUrlParameter), databaseUsername, databasePassword);

        Statement stmt = conn.createStatement();
        String sql;


        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ResultSet rs;
        //step 1 : load them all
        sql = "select * from virtual_machine_reporting_action order by timestamp asc;";
        rs = stmt.executeQuery(sql);

        List<VMActionsDTO> tmpVMActionsList = new ArrayList<>();

        int localMax = 0;
        while (rs.next()) {
            VMActionsDTO dto = new VMActionsDTO();
            Date timestamp = simpleDateFormat.parse(rs.getString("timestamp"));
            String vmID = rs.getString("virtual_machineid");
            String vmAction = rs.getString("vm_action");
            String vmTypeId = rs.getString("virtual_machine_typeid");
            if (isBaseline) {
                //    vmID = vmID.replace("0_", "3_");
            }

            dto.setVMID(vmID);
            dto.setVMAction(vmAction);
            dto.setDate(timestamp);
            dto.setVMTypeID(vmTypeId);
            dto.setDate(new Date(timestamp.getTime() - firstDate.getTime()));
            tmpVMActionsList.add(dto);
        }


        //step3: fill in missing entries:
        GregorianCalendar start = new GregorianCalendar();
        long time = firstDate.getTime();
        start.setTime(new Date(time - time));

        //sort per minute
        Map<Integer, List<VMActionsDTO>> perMinuteMap = new HashMap<>();

        for (VMActionsDTO vmActionsDTO : tmpVMActionsList) {
            Calendar current = new GregorianCalendar();
            current.setTime(vmActionsDTO.getDate());
            current.set(Calendar.SECOND, 0);
            vmActionsDTO.setDate(current.getTime());
            int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsDTO.getDate().getTime() - 0);
            List<VMActionsDTO> vmActionsDTOs = perMinuteMap.get(minutes);
            if (vmActionsDTOs == null) {
                vmActionsDTOs = new ArrayList<>();
            }
            if (vmActionsDTO.getVMAction().equalsIgnoreCase("START")) {
                localMax = Math.max(minutes, localMax);
            }
            vmActionsDTOs.add(vmActionsDTO);
            perMinuteMap.put(minutes, vmActionsDTOs);
        }


        VMActionsDTO lastAction = new VMActionsDTO();
        lastAction.setDate(start.getTime());
        lastAction.setVMAction("");
        lastAction.setCoreAmount(0);
        lastAction.setVMID("");
//        System.out.println("LastMinute: " + maxDurationInMinutes);

        //if last action (localMax) was less than 5 minutes before from the maxDurationInMinutes, set the maxDurationInMinutes to lastAction+5

        if ((localMax) > maxDurationInMinutes) {
            //ignore
        } else if ((localMax + 5) > maxDurationInMinutes) {
            maxDurationInMinutes = localMax + (int) (Math.ceil((maxDurationInMinutes - localMax) / 5.0) * 5);
        } else {
            maxDurationInMinutes = localMax + (int) (Math.ceil((maxDurationInMinutes - localMax) / 5.0) * 5);
        }


        for (int i = 0; i <= maxDurationInMinutes; i++) {
            List<VMActionsDTO> vmActionsDTOs = perMinuteMap.get(i);
            if (vmActionsDTOs == null) {
                vmActionsDTOs = new ArrayList<>();
            }
            if (vmActionsDTOs.size() == 0) {
                if (i == 0) {
                    VMActionsDTO clone = copy(lastAction);
                    vmActionsDTOs.add(clone);
                    perMinuteMap.put(i, vmActionsDTOs);
                    continue;
                }
                lastAction = copy(lastAction);
                GregorianCalendar current = new GregorianCalendar();
                current.setTime(lastAction.getDate());
                int minutes = current.get(Calendar.MINUTE);
                current.set(Calendar.MINUTE, minutes + 1);
                lastAction.setDate(current.getTime());
                if (i >= maxDurationInMinutes) {
                    lastAction.setCoreAmount(0);
                }

                vmActionsDTOs.add(lastAction);
            } else {
                VMActionsDTO newAction = new VMActionsDTO();
                VMActionsDTO first = vmActionsDTOs.get(0);
                newAction.setDate(first.getDate());
                newAction.setVMID(lastAction.getVMID() + ",");
                int sum = 0;
                for (VMActionsDTO vmActionsDTO : vmActionsDTOs) {
                    String vmid = newAction.getVMID() + vmActionsDTO.getVMID() + "_" + vmActionsDTO.getVMAction() + ",";
                    newAction.setVMID(vmid);
                    sum += getCoreCount(vmActionsDTO);
                }
                sum = sum + lastAction.getCoreAmount();
                if (i >= maxDurationInMinutes) {//set last value to 0
                    newAction.setCoreAmount(0);
                } else {
                    newAction.setCoreAmount(sum);
                }
                vmActionsDTOs = new ArrayList<>();
                vmActionsDTOs.add(newAction);
                lastAction = copy(newAction);
            }

            perMinuteMap.put(i, vmActionsDTOs);
        }

        List<VMActionsDTO> res = new ArrayList<>();
        for (Integer integer : perMinuteMap.keySet()) {
            if (integer > maxDurationInMinutes) {
                continue; // no need to add them
            }
            res.addAll(perMinuteMap.get(integer));
        }
        return res;
    }


    private VMActionsDTO copy(VMActionsDTO lastAction) {

        VMActionsDTO copy = new VMActionsDTO();
        copy.setDate(lastAction.getDate());
        copy.setVMID(lastAction.getVMID());
        copy.setCoreAmount(lastAction.getCoreAmount());
        copy.setVMAction(lastAction.getVMAction());
        copy.setVMTypeID(lastAction.getVMTypeID());
        return copy;
    }



    /**
     * @param isBaseline if this is true, than replace the 0_ variable name with 3_, since in baseline only quadcores were allowed
     * @return [0] internal costs; [1] external costs
     * @throws SQLException
     * @throws ParseException
     */
    public double[] getCoreUsage(String dbName, WorkflowDTO firstArrivedWorkflow, WorkflowDTO lastArrivedWorkflow, boolean isBaseline) throws SQLException, ParseException {
        Double internalCosts = 0.0;
        Double externalCosts = 0.0;


        Connection conn = DriverManager.getConnection(databaseUrl.concat(dbName).concat(databaseUrlParameter), databaseUsername, databasePassword);

        Statement stmt = conn.createStatement();
        String sql;
        sql = "select * from virtual_machine_reporting_action;";
        ResultSet rs = stmt.executeQuery(sql);


        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //STEP 5: Extract data from result set

        List<VMActionsDTO> results = new ArrayList<>();


        while (rs.next()) {
            //Retrieve by column name
            VMActionsDTO dto = new VMActionsDTO();
            Date timestamp = simpleDateFormat.parse(rs.getString("timestamp"));
            String vmID = rs.getString("virtual_machineid");
            String vmAction = rs.getString("vm_action");
            String vmTypeId = rs.getString("virtual_machine_typeid");
            if (isBaseline) {
                //    vmID = vmID.replace("0_", "3_");
            }
            dto.setVMID(vmID);
            dto.setVMAction(vmAction);
            dto.setDate(timestamp);
            dto.setVMTypeID(vmTypeId);
            //dto.setDate(new Date(timestamp.getTime() - firstArrivedWorkflow.getArrivedAt().getTime()));
            results.add(dto);
        }

        Collections.sort(results, Comparator.comparing(VMActionsDTO::getDate));

        List<VMActionsDTO> stopResults = new ArrayList<>();
        for (VMActionsDTO result : results) {
            if (result.getVMAction().equalsIgnoreCase("STOPPED")) {
                stopResults.add(result);
            }
        }
//        stopResults.addAll(results);

        for (VMActionsDTO action : results) {

            if (action.getVMAction().equalsIgnoreCase("START")) {
                long milliseconds = 0;
                for (VMActionsDTO action2 : stopResults) {
                    if (action2.getVMID().equals(action.getVMID())) {
                        milliseconds = action2.getDate().getTime() - action.getDate().getTime();
                        stopResults.remove(action2);
                        break;
                    }
                }
                if (milliseconds == 0) {
                    milliseconds = lastArrivedWorkflow.getFinishedAt().getTime() - action.getDate().getTime();
                }
                double inMinutes = milliseconds / 1000 / 60;
                double timeslots = Math.ceil(inMinutes / 5);

                try {
                    VMType vmType = cacheVirtualMachineService.getVmTypeFromIdentifier(action.getVMTypeID());


                    if(vmType.getLocation().equals("internal")) {
                        internalCosts = internalCosts + (vmType.getCosts() * timeslots);
                    } else {
                        externalCosts = externalCosts + (vmType.getCosts() * timeslots);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

        System.out.println(dbName + " core costs: " + (internalCosts + externalCosts) + " - internalCosts: " + internalCosts + " - externalCosts: " + externalCosts);
        rs.close();
        stmt.close();

        return new double[]{internalCosts, externalCosts};
    }

    public int getCoreCount(VMActionsDTO vmAction) {

        int result = 1;

        try {
            VMType vmType = cacheVirtualMachineService.getVmTypeFromIdentifier(vmAction.getVMTypeID());
            result = vmType.getCores();
        } catch (Exception e) {
            e.printStackTrace();
        }

        result = (!vmAction.getVMAction().equalsIgnoreCase("START")) ? result * -1 : result;
        return result;
    }

}
