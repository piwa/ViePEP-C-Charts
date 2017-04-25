package at.ac.tuwien.infosys.viepepc;

import at.ac.tuwien.infosys.viepepc.database.entities.VMActionsDTO;
import at.ac.tuwien.infosys.viepepc.database.entities.WorkflowDTO;
import at.ac.tuwien.infosys.viepepc.database.services.DataTransferElementService;
import at.ac.tuwien.infosys.viepepc.database.services.VMActionsService;
import at.ac.tuwien.infosys.viepepc.database.services.WorkflowService;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Philipp Hoenisch on 9/1/14.
 */
@Component
public class DataLoader {

    @Autowired
    private JFreeChartCreator jFreeChartCreator;
    @Autowired
    private VMActionsService vmActionsService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private DataTransferElementService dataTransferElementService;

    @Value("${spring.datasource.driver-class-instanceId}")
    private String databaseDriver = "com.mysql.jdbc.Driver";

    private TimeSeriesCollection workflowArrivalDataSet = null;
    private TimeSeriesCollection optimizedVMDataSet = null;

    public void createGraph() {
        Statement stmt = null;
        try {
            //STEP 2: Register JDBC driver
            Class.forName(databaseDriver);

            //STEP 3: Open a connection
            System.out.println("Connecting to database...");

            JFreeChartCreator jFreeChartCreator = new JFreeChartCreator();
            List<File> files = new ArrayList<>();
            // int mo=0;
            for (int mo = 0; mo < 2; mo++) {

                int predefinedMax = 60;
                String chartName = "Constant Arrival - Strict";
                String optimizedRun = "viepep4";
                String baselineRun = "viepep4";
                //String baselineRun = "run%sconstant15baseline";
                // String filename = "/tmp/figure181827937803145453.pdf";
                String filename = "figure181827937803145453.pdf";
/*
                switch (mo) {

                    case 0:
                        predefinedMax = 40;
                        chartName = "Constant Arrival - Strict";
                       // optimizedRun = "run%sburst15";
                      optimizedRun="viepep3_const1_25_120sec_100proc_18vms_genetic_1";
                    // optimizedRun="viepep3_const1_25_120sec_18vms_genetic_2";//very nice to show!!!
                      baselineRun="viepep3_const1_25_120sec_18vms";//
                        //  baselineRun="viepep3_const1_25_120sec_9vms";

                      //  baselineRun="viepep3_constant_factor1_25_120sec";
                       // baselineRun = "run%sconstant15baseline";
                        filename = "constant15.pdf";
                        break;
                    case 1:
                        predefinedMax = 40;
                        chartName = "Constant Arrival - Lenient";
                        optimizedRun = "viepep3_const1_25_120sec_18vms_genetic_100proc";//meaning simulation-false
                        baselineRun="viepep3_const1_25_120sec_18vms_100proc";
                       // baselineRun = "run%sconstant25baseline";
                        filename = "constant25.pdf";
                        break;
                    case 2:
                        predefinedMax = 65;
                        chartName = "Pyramid Arrival - Strict";
                        optimizedRun = "viepep3";
                        baselineRun="viepep3";
                      //  baselineRun = "run%spyramid15baseline";
                        filename = "pyramid15.pdf";
                        break;
                    case 3:
                        predefinedMax = 65;
                        chartName = "Pyramid Arrival - Lenient";
                        optimizedRun = "viepep3";
                        baselineRun="viepep3";
                       // baselineRun = "run%spyramid25baseline";
                        filename = "pyramid25.pdf";
                        break;

                }
*/



                int maxOptimizedDuration = 0;
                int maxBaselineDuration = 0;

                durationOptimized(optimizedRun, maxOptimizedDuration);

                durationBaseline(baselineRun, maxBaselineDuration);

                int absolutMax = predefinedMax; //Math.max(maxBaselineDuration, maxOptimizedDuration);

                JFreeChart chart = jFreeChartCreator.createChart(chartName, workflowArrivalDataSet, optimizedVMDataSet, new Date(absolutMax * 61 * 1000));
                OutputStream out = null;
                try {
                    File file = new File(filename);
                    if (!file.exists()) {
                        boolean newFile = file.createNewFile();
                    }
                    out = new FileOutputStream(file);
                    jFreeChartCreator.writeAsPDF(chart, out, 500, 270);
                    System.out.println("IMG at: " + file.getAbsolutePath());
                    files.add(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                for (File file : files) {
                    System.out.println("cp "+ file.getAbsolutePath() + " .");
                }
            }//here from cycle??
        } catch (SQLException se) {
            //Handle errors for JDBC
            se.printStackTrace();
        } catch (Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
        } finally {
            //finally block used to close resources
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException se2) {
            }// nothing we can do
        }//end try

        System.out.println("Goodbye!");
    }//end main


    private void durationBaseline(String baselineRun, int maxBaselineDuration) throws SQLException, ParseException {
        List<WorkflowDTO> workflowsRun1 = workflowService.getWorkflowDTOs(String.format(baselineRun, "1"));
        List<WorkflowDTO> workflowsRun2 = workflowService.getWorkflowDTOs(String.format(baselineRun, "2"));
        List<WorkflowDTO> workflowsRun3 = workflowService.getWorkflowDTOs(String.format(baselineRun, "3"));
        WorkflowDTO lastExecutedWorkflowRun1 = workflowService.getLastExecutedWorkflow(workflowsRun1);
        WorkflowDTO lastExecutedWorkflowRun2 = workflowService.getLastExecutedWorkflow(workflowsRun2);
        WorkflowDTO lastExecutedWorkflowRun3 = workflowService.getLastExecutedWorkflow(workflowsRun3);

        int dif1 = (int) getDurationInMinutes(workflowsRun1.get(0), lastExecutedWorkflowRun1);
        int dif2 = (int) getDurationInMinutes(workflowsRun2.get(0), lastExecutedWorkflowRun2);
        int dif3 = (int) getDurationInMinutes(workflowsRun3.get(0), lastExecutedWorkflowRun3);
        maxBaselineDuration = (int) Math.max(Math.max(Math.max(dif1, dif2), dif3), 0);
//                maxBaselineDuration = (int) Math.ceil(maxBaselineDuration / 5.0) * 5;
        calculateStandardDeviation("duration baselineRun", dif1, dif2, dif3);

        List<VMActionsDTO> vmActionsRun1 = vmActionsService.getVMActionsDTOs(String.format(baselineRun, "1"), workflowsRun1.get(0).getArrivedAt(), dif1, true);
        List<VMActionsDTO> vmActionsRun2 = vmActionsService.getVMActionsDTOs(String.format(baselineRun, "2"), workflowsRun2.get(0).getArrivedAt(), dif2, true);
        List<VMActionsDTO> vmActionsRun3 = vmActionsService.getVMActionsDTOs(String.format(baselineRun, "3"), workflowsRun3.get(0).getArrivedAt(), dif3, true);

        Collections.sort(vmActionsRun1, Comparator.comparing(VMActionsDTO::getDate));
        Collections.sort(vmActionsRun2, Comparator.comparing(VMActionsDTO::getDate));
        Collections.sort(vmActionsRun3, Comparator.comparing(VMActionsDTO::getDate));

        int minute1 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun1.get(vmActionsRun1.size() - 1).getDate().getTime());
        int minute2 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun2.get(vmActionsRun2.size() - 1).getDate().getTime());
        int minute3 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun3.get(vmActionsRun3.size() - 1).getDate().getTime());
        maxBaselineDuration = Math.max(minute1, Math.max(minute2, Math.max(minute3, maxBaselineDuration)));
        maxBaselineDuration = (int) Math.ceil(maxBaselineDuration / 5.0) * 5;

//                workflowArrivalDataSet = jFreeChartCreator.createArrivalDataSet(workflowsRun1);
        TimeSeriesCollection baselineVMDataSet = jFreeChartCreator.createVMDataSet("Baseline", vmActionsRun1, vmActionsRun2, vmActionsRun3);

        List series = baselineVMDataSet.getSeries();
        for (Object serie : series) {
            optimizedVMDataSet.addSeries((TimeSeries) serie);
        }

        double[] coreUsage1 = vmActionsService.getCoreUsage(String.format(baselineRun, "1"), workflowsRun1.get(0), lastExecutedWorkflowRun1, true);
        double[] coreUsage2 = vmActionsService.getCoreUsage(String.format(baselineRun, "2"), workflowsRun2.get(0), lastExecutedWorkflowRun2, true);
        double[] coreUsage3 = vmActionsService.getCoreUsage(String.format(baselineRun, "3"), workflowsRun3.get(0), lastExecutedWorkflowRun3, true);
        calculateStandardDeviation("core usage", coreUsage1[0], coreUsage2[0], coreUsage3[0]);
        calculateStandardDeviation("core usage", coreUsage1[1], coreUsage2[1], coreUsage3[1]);

        double transferCosts1 = dataTransferElementService.getDataTransferCosts(String.format(baselineRun, "1"));
        double transferCosts2 = dataTransferElementService.getDataTransferCosts(String.format(baselineRun, "2"));
        double transferCosts3 = dataTransferElementService.getDataTransferCosts(String.format(baselineRun, "3"));
        calculateStandardDeviation("transfer costs", transferCosts1, transferCosts2, transferCosts3);

        double[] penaltyPoints1 = penalty(workflowsRun1);
        double[] penaltyPoints2 = penalty(workflowsRun2);
        double[] penaltyPoints3 = penalty(workflowsRun3);
        calculateStandardDeviation("penalty percent", penaltyPoints1[0], penaltyPoints2[0], penaltyPoints3[0]);
        calculateStandardDeviation("penalty points", penaltyPoints1[1], penaltyPoints2[1], penaltyPoints3[1]);

        double total1 = coreUsage1[0] + penaltyPoints1[1];
        double total2 = coreUsage2[0] + penaltyPoints2[1];
        double total3 = coreUsage3[0] + penaltyPoints3[1];

        System.out.println("total costs1: " + total1);
        System.out.println("total costs2: " + total2);
        System.out.println("total costs3: " + total3);
        calculateStandardDeviation("total costs", total1, total2, total3);
    }


    private int durationOptimized(String optimizedRun, int maxOptimizedDuration) throws SQLException, ParseException {
        List<WorkflowDTO> workflowsRun1 = workflowService.getWorkflowDTOs(String.format(optimizedRun, "1"));
        List<WorkflowDTO> workflowArrivals = workflowService.getWorkflowDTOsArrivals(String.format(optimizedRun, "1"));
        List<WorkflowDTO> workflowsRun2 = workflowService.getWorkflowDTOs(String.format(optimizedRun, "2"));
        List<WorkflowDTO> workflowsRun3 = workflowService.getWorkflowDTOs(String.format(optimizedRun, "3"));
        WorkflowDTO lastExecutedWorkflowRun1 = workflowService.getLastExecutedWorkflow(workflowsRun1);
        WorkflowDTO lastExecutedWorkflowRun2 = workflowService.getLastExecutedWorkflow(workflowsRun2);
        WorkflowDTO lastExecutedWorkflowRun3 = workflowService.getLastExecutedWorkflow(workflowsRun3);

        int dif1 = (int) getDurationInMinutes(workflowsRun1.get(0), lastExecutedWorkflowRun1);
        int dif2 = (int) getDurationInMinutes(workflowsRun2.get(0), lastExecutedWorkflowRun2);
        int dif3 = (int) getDurationInMinutes(workflowsRun3.get(0), lastExecutedWorkflowRun3);
        maxOptimizedDuration = (int) Math.max(Math.max(dif1, dif2), dif3);
//                maxOptimizedDuration = (int) Math.ceil(maxOptimizedDuration / 5.0) * 5;

        calculateStandardDeviation("duration optimized", dif1, dif2, dif3);


        List<VMActionsDTO> vmActionsRun1 = vmActionsService.getVMActionsDTOs(String.format(optimizedRun, "1"), workflowsRun1.get(0).getArrivedAt(), dif1, false);
        List<VMActionsDTO> vmActionsRun2 = vmActionsService.getVMActionsDTOs(String.format(optimizedRun, "2"), workflowsRun2.get(0).getArrivedAt(), dif2, false);
        List<VMActionsDTO> vmActionsRun3 = vmActionsService.getVMActionsDTOs(String.format(optimizedRun, "3"), workflowsRun3.get(0).getArrivedAt(), dif3, false);
        Collections.sort(vmActionsRun1, Comparator.comparing(VMActionsDTO::getDate));
        Collections.sort(vmActionsRun2, Comparator.comparing(VMActionsDTO::getDate));
        Collections.sort(vmActionsRun3, Comparator.comparing(VMActionsDTO::getDate));

        int minute1 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun1.get(vmActionsRun1.size() - 1).getDate().getTime());
        int minute2 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun2.get(vmActionsRun2.size() - 1).getDate().getTime());
        int minute3 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun3.get(vmActionsRun3.size() - 1).getDate().getTime());
        maxOptimizedDuration = Math.max(minute1, Math.max(minute2, Math.max(minute3, maxOptimizedDuration)));
//                maxOptimizedDuration = (int) Math.ceil(maxOptimizedDuration / 5.0) * 5;


        workflowArrivalDataSet = jFreeChartCreator.createArrivalDataSet(workflowArrivals);
        optimizedVMDataSet = jFreeChartCreator.createVMDataSet("SIPP", vmActionsRun1, vmActionsRun2, vmActionsRun3);

        double[] coreUsage1 = vmActionsService.getCoreUsage(String.format(optimizedRun, "1"), workflowsRun1.get(0), lastExecutedWorkflowRun1, false);
        double[] coreUsage2 = vmActionsService.getCoreUsage(String.format(optimizedRun, "2"), workflowsRun2.get(0), lastExecutedWorkflowRun2, false);
        double[] coreUsage3 = vmActionsService.getCoreUsage(String.format(optimizedRun, "3"), workflowsRun3.get(0), lastExecutedWorkflowRun3, false);
        calculateStandardDeviation("core usage", coreUsage1[0], coreUsage2[0], coreUsage3[0]);
        calculateStandardDeviation("core usage", coreUsage1[1], coreUsage2[1], coreUsage3[1]);

//                    double transferCosts1 = getDataTransferCosts(String.format(optimizedRun, "1"));
//                    double transferCosts2 = getDataTransferCosts(String.format(optimizedRun, "2"));
//                    double transferCosts3 = getDataTransferCosts(String.format(optimizedRun, "3"));
//                    calculateStandardDeviation("transfer costs", transferCosts1, transferCosts2, transferCosts3);

        double[] penaltyPoints1 = penalty(workflowsRun1);
        double[] penaltyPoints2 = penalty(workflowsRun2);
        double[] penaltyPoints3 = penalty(workflowsRun3);
        calculateStandardDeviation("penalty percent", penaltyPoints1[0], penaltyPoints2[0], penaltyPoints3[0]);
        calculateStandardDeviation("penalty points", penaltyPoints1[1], penaltyPoints2[1], penaltyPoints3[1]);

        double total1 = coreUsage1[0] + penaltyPoints1[1];
        double total2 = coreUsage2[0] + penaltyPoints2[1];
        double total3 = coreUsage3[0] + penaltyPoints3[1];


        System.out.println("total costs1: " + total1);
        System.out.println("total costs2: " + total2);
        System.out.println("total costs3: " + total3);
        calculateStandardDeviation("total costs", total1, total2, total3);

        return maxOptimizedDuration;
    }


    private static void calculateStandardDeviation(String field, double... values) {
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (double value : values) {
            stats.addValue(value);
        }

        // Compute some statistics
        double mean = stats.getMean();
        double std = stats.getStandardDeviation();
        System.out.println(field + ": average" + " mean: " + (mean) + " - std: " + std);
    }

    private static long getDurationInMinutes(WorkflowDTO workflowDTO, WorkflowDTO lastExecutedWorkflowRun1) {
        long start = workflowDTO.getArrivedAt().getTime();
        long end = lastExecutedWorkflowRun1.getFinishedAt().getTime();
        return TimeUnit.MILLISECONDS.toMinutes(end - start);
    }



    public double[] penalty(List<WorkflowDTO> workflowsRun) throws SQLException {
        double[] results = new double[2];
        double penalityPoints = 0;
        double missedDeadlines = 0;
        double totalDeadlines = 0;

        double percentage;
        for (WorkflowDTO wf : workflowsRun) {
            totalDeadlines++;

            Double overallDuration = Double.valueOf(wf.getFinishedAt().getTime() - wf.getArrivedAt().getTime());
            Double timediff = Double.valueOf(wf.getFinishedAt().getTime() - wf.getDeadline().getTime());
            //  System.out.println("FinishedAt: " + wf.getFinishedAt().getTime() + "ArrivedAt: " + wf.getArrivedAt().getTime() + "Difference: " + timediff);
            if (timediff > 0) {
                penalityPoints += Math.ceil((timediff / overallDuration) * 10);
                missedDeadlines++;
            }
        }
        percentage = (100.0 / workflowsRun.size()) * missedDeadlines;
        System.out.println("MissedDeadlines: " + missedDeadlines + "/" + totalDeadlines + "(" + percentage + "%) -- Penalty points: " + penalityPoints);
        return new double[]{percentage, penalityPoints};
    }

}//end FirstExample