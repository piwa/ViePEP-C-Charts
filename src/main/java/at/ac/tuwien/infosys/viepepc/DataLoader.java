package at.ac.tuwien.infosys.viepepc;

import at.ac.tuwien.infosys.viepepc.database.entities.VMActionsDTO;
import at.ac.tuwien.infosys.viepepc.database.entities.WorkflowDTO;
import at.ac.tuwien.infosys.viepepc.database.services.DataTransferElementService;
import at.ac.tuwien.infosys.viepepc.database.services.VMActionsService;
import at.ac.tuwien.infosys.viepepc.database.services.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.time.DateTimeException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Philipp Hoenisch on 9/1/14.
 */
@Component
@Slf4j
public class DataLoader {

    @Autowired
    @Qualifier("jFreeChartCreator")
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
    private String suffix1;
    private String suffix2;
    private String suffix3;

    public ChartDataHolder createGraphMain(int predefinedMax, int coreAxisSteps, int maxCoreAxisValue, String chartName, String optimizedRun, String baselineRun, String filename, String suffix1, String suffix2, String suffix3) {
        Statement stmt = null;
        try {
            Class.forName(databaseDriver);
            log.info("Connecting to database...");

            this.suffix1 = suffix1;
            this.suffix2 = suffix2;
            this.suffix3 = suffix3;

            return createGraph(predefinedMax, coreAxisSteps, maxCoreAxisValue, chartName, optimizedRun, baselineRun, filename);

        } catch (SQLException se) {

            se.printStackTrace();
        } catch (Exception e) {

            e.printStackTrace();
        } finally {

            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException se2) {
            }
        }

        return null;
    }

    public ChartDataHolder createGraph(int predefinedMax, int coreAxisSteps, int maxCoreAxisValue, String chartName, String optimizedRun, String baselineRun, String filename) throws SQLException, ParseException, IOException {


        int maxOptimizedDuration = 0;
        int maxBaselineDuration = 0;

        durationOptimized(optimizedRun, maxOptimizedDuration);
        log.info("");
        durationBaseline(baselineRun, maxBaselineDuration);


        int absolutMax = predefinedMax;

        JFreeChart chart = jFreeChartCreator.createChart(chartName, workflowArrivalDataSet, optimizedVMDataSet, new Date(absolutMax * 1000), maxCoreAxisValue, coreAxisSteps);
        OutputStream out = null;
        try {
            File file = new File(filename);
            if (!file.exists()) {
                boolean newFile = file.createNewFile();
            }
            out = new FileOutputStream(file);
            jFreeChartCreator.writeAsPDF(chart, out, 500, 350);
            log.info("IMG at: " + file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        log.info("");


        return new ChartDataHolder(chartName, workflowArrivalDataSet, optimizedVMDataSet, new Date(absolutMax * 1000), maxCoreAxisValue, coreAxisSteps);

    }


    private int durationOptimized(String optimizedRun, int maxOptimizedDuration) throws SQLException, ParseException {

        log.info("----------------- Optimization: " + optimizedRun + " -----------------");

        List<WorkflowDTO> workflowArrivals = workflowService.getWorkflowDTOsArrivals(String.format(optimizedRun, suffix1));

        List<WorkflowDTO> workflowsRun1 = workflowService.getWorkflowDTOs(String.format(optimizedRun, suffix1));
        List<WorkflowDTO> workflowsRun2 = workflowService.getWorkflowDTOs(String.format(optimizedRun, suffix2));
        List<WorkflowDTO> workflowsRun3 = workflowService.getWorkflowDTOs(String.format(optimizedRun, suffix3));
        WorkflowDTO lastExecutedWorkflowRun1 = workflowService.getLastExecutedWorkflow(workflowsRun1);
        WorkflowDTO lastExecutedWorkflowRun2 = workflowService.getLastExecutedWorkflow(workflowsRun2);
        WorkflowDTO lastExecutedWorkflowRun3 = workflowService.getLastExecutedWorkflow(workflowsRun3);

        int eval1Duration = (int) getDurationInSeconds(workflowsRun1.get(0), lastExecutedWorkflowRun1);
        int eval2Duration = (int) getDurationInSeconds(workflowsRun2.get(0), lastExecutedWorkflowRun2);
        int eval3Duration = (int) getDurationInSeconds(workflowsRun3.get(0), lastExecutedWorkflowRun3);
        maxOptimizedDuration = Math.max(Math.max(eval1Duration, eval2Duration), eval3Duration);

        calculateStandardDeviation("Execution duration with optimization", eval1Duration, eval2Duration, eval3Duration);

        List<VMActionsDTO> vmActionsRun1 = vmActionsService.getVMActionsDTOs(String.format(optimizedRun, suffix1), workflowsRun1.get(0).getArrivedAt(), eval1Duration, false);
        List<VMActionsDTO> vmActionsRun2 = vmActionsService.getVMActionsDTOs(String.format(optimizedRun, suffix2), workflowsRun2.get(0).getArrivedAt(), eval2Duration, false);
        List<VMActionsDTO> vmActionsRun3 = vmActionsService.getVMActionsDTOs(String.format(optimizedRun, suffix3), workflowsRun3.get(0).getArrivedAt(), eval3Duration, false);
        Collections.sort(vmActionsRun1, Comparator.comparing(VMActionsDTO::getDate));
        Collections.sort(vmActionsRun2, Comparator.comparing(VMActionsDTO::getDate));
        Collections.sort(vmActionsRun3, Comparator.comparing(VMActionsDTO::getDate));

        int second1 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun1.get(vmActionsRun1.size() - 1).getDate().getTime());
        int second2 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun2.get(vmActionsRun2.size() - 1).getDate().getTime());
        int second3 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun3.get(vmActionsRun3.size() - 1).getDate().getTime());
        maxOptimizedDuration = Math.max(second1, Math.max(second2, Math.max(second3, maxOptimizedDuration)));

        double[] coreUsage1 = vmActionsService.getCoreUsage(String.format(optimizedRun, suffix1), workflowsRun1.get(0), lastExecutedWorkflowRun1, false);
        double[] coreUsage2 = vmActionsService.getCoreUsage(String.format(optimizedRun, suffix2), workflowsRun2.get(0), lastExecutedWorkflowRun2, false);
        double[] coreUsage3 = vmActionsService.getCoreUsage(String.format(optimizedRun, suffix3), workflowsRun3.get(0), lastExecutedWorkflowRun3, false);
//        calculateStandardDeviation("Intern core usage", coreUsage1[0], coreUsage2[0], coreUsage3[0]);
        calculateStandardDeviation("Extern core usage", coreUsage1[1], coreUsage2[1], coreUsage3[1]);

        double[] penaltyPoints1 = penalty(workflowsRun1, suffix1);
        double[] penaltyPoints2 = penalty(workflowsRun2, suffix2);
        double[] penaltyPoints3 = penalty(workflowsRun3, suffix3);
        calculateStandardDeviation("SLA adherence percent", penaltyPoints1[0], penaltyPoints2[0], penaltyPoints3[0]);
        calculateStandardDeviation("Penalty points", penaltyPoints1[1], penaltyPoints2[1], penaltyPoints3[1]);

        double total1 = coreUsage1[0] + coreUsage1[1] + penaltyPoints1[1];
        double total2 = coreUsage2[0] + coreUsage2[1] + penaltyPoints2[1];
        double total3 = coreUsage3[0] + coreUsage3[1] + penaltyPoints3[1];


        log.info("Total costs of optimized run 1: " + total1);
        log.info("Total costs of optimized run 2: " + total2);
        log.info("Total costs of optimized run 3: " + total3);
        calculateStandardDeviation("Total costs optimization runs ", total1, total2, total3);

        workflowArrivalDataSet = jFreeChartCreator.createArrivalDataSet(workflowArrivals);
        optimizedVMDataSet = jFreeChartCreator.createVMDataSet("GeCoVM", vmActionsRun1, vmActionsRun2, vmActionsRun3);

        return maxOptimizedDuration;
    }


    private void durationBaseline(String baselineRun, int maxBaselineDuration) throws SQLException, ParseException {

        log.info("----------------- Baseline: " + baselineRun + " -----------------");

        List<WorkflowDTO> workflowsRun1 = workflowService.getWorkflowDTOs(String.format(baselineRun, suffix1));
        List<WorkflowDTO> workflowsRun2 = workflowService.getWorkflowDTOs(String.format(baselineRun, suffix2));
        List<WorkflowDTO> workflowsRun3 = workflowService.getWorkflowDTOs(String.format(baselineRun, suffix3));
        WorkflowDTO lastExecutedWorkflowRun1 = workflowService.getLastExecutedWorkflow(workflowsRun1);
        WorkflowDTO lastExecutedWorkflowRun2 = workflowService.getLastExecutedWorkflow(workflowsRun2);
        WorkflowDTO lastExecutedWorkflowRun3 = workflowService.getLastExecutedWorkflow(workflowsRun3);

        int eval1Duration = (int) getDurationInSeconds(workflowsRun1.get(0), lastExecutedWorkflowRun1);
        int eval2Duration = (int) getDurationInSeconds(workflowsRun2.get(0), lastExecutedWorkflowRun2);
        int eval3Duration = (int) getDurationInSeconds(workflowsRun3.get(0), lastExecutedWorkflowRun3);
        maxBaselineDuration = (int) Math.max(Math.max(Math.max(eval1Duration, eval2Duration), eval3Duration), 0);
//                maxBaselineDuration = (int) Math.ceil(maxBaselineDuration / 5.0) * 5;
        calculateStandardDeviation("Execution duration baseline run", eval1Duration, eval2Duration, eval3Duration);

        Date arrived1 = new Date(workflowsRun1.get(0).getArrivedAt().getTime() - 000);
        Date arrived2 = new Date(workflowsRun2.get(0).getArrivedAt().getTime() - 000);
        Date arrived3 = new Date(workflowsRun3.get(0).getArrivedAt().getTime() - 000);
//        Date first = workflowsRun1.get(0).getArrivedAt();

        List<VMActionsDTO> vmActionsRun1 = vmActionsService.getVMActionsDTOs(String.format(baselineRun, suffix1), arrived1, eval1Duration, true);
        List<VMActionsDTO> vmActionsRun2 = vmActionsService.getVMActionsDTOs(String.format(baselineRun, suffix2), arrived2, eval2Duration, true);
        List<VMActionsDTO> vmActionsRun3 = vmActionsService.getVMActionsDTOs(String.format(baselineRun, suffix3), arrived3, eval3Duration, true);

        Collections.sort(vmActionsRun1, Comparator.comparing(VMActionsDTO::getDate));
        Collections.sort(vmActionsRun2, Comparator.comparing(VMActionsDTO::getDate));
        Collections.sort(vmActionsRun3, Comparator.comparing(VMActionsDTO::getDate));

        int second1 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun1.get(vmActionsRun1.size() - 1).getDate().getTime());
        int second2 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun2.get(vmActionsRun2.size() - 1).getDate().getTime());
        int second3 = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsRun3.get(vmActionsRun3.size() - 1).getDate().getTime());
        maxBaselineDuration = Math.max(second1, Math.max(second2, Math.max(second3, maxBaselineDuration)));
        maxBaselineDuration = (int) Math.ceil(maxBaselineDuration / 5.0) * 5;

//                workflowArrivalDataSet = jFreeChartCreator.createArrivalDataSet(workflowsRun1);
        TimeSeriesCollection baselineVMDataSet = jFreeChartCreator.createVMDataSet("Baseline", vmActionsRun1, vmActionsRun2, vmActionsRun3);

        List series = baselineVMDataSet.getSeries();
        series.forEach(serie -> optimizedVMDataSet.addSeries((TimeSeries) serie));

        double[] coreUsage1 = vmActionsService.getCoreUsage(String.format(baselineRun, suffix1), workflowsRun1.get(0), lastExecutedWorkflowRun1, true);
        double[] coreUsage2 = vmActionsService.getCoreUsage(String.format(baselineRun, suffix2), workflowsRun2.get(0), lastExecutedWorkflowRun2, true);
        double[] coreUsage3 = vmActionsService.getCoreUsage(String.format(baselineRun, suffix3), workflowsRun3.get(0), lastExecutedWorkflowRun3, true);
//        calculateStandardDeviation("Intern Core usage", coreUsage1[0], coreUsage2[0], coreUsage3[0]);
        calculateStandardDeviation("Extern Core usage", coreUsage1[1], coreUsage2[1], coreUsage3[1]);

        double[] penaltyPoints1 = penalty(workflowsRun1, suffix1);
        double[] penaltyPoints2 = penalty(workflowsRun2, suffix2);
        double[] penaltyPoints3 = penalty(workflowsRun3, suffix3);
        calculateStandardDeviation("SLA adherence percent", penaltyPoints1[0], penaltyPoints2[0], penaltyPoints3[0]);
        calculateStandardDeviation("Penalty points", penaltyPoints1[1], penaltyPoints2[1], penaltyPoints3[1]);

        double total1 = coreUsage1[0] + coreUsage1[1] + penaltyPoints1[1];
        double total2 = coreUsage2[0] + coreUsage2[1] + penaltyPoints2[1];
        double total3 = coreUsage3[0] + coreUsage3[1] + penaltyPoints3[1];

        log.info("Total costs of baseline 1: " + total1);
        log.info("Total costs of baseline 2: " + total2);
        log.info("Total costs of baseline 3: " + total3);
        calculateStandardDeviation("Total costs baseline", total1, total2, total3);
    }


    private static void calculateStandardDeviation(String field, double... values) {
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (double value : values) {
//            value = value + (new Random()).ints(-20, 20).findFirst().getAsInt();
            stats.addValue(value);
        }

        // Compute some statistics
        double mean = stats.getMean();
        double std = stats.getStandardDeviation();

        if(field.equals("Penalty percent") || field.equals("SLA adherence percent")) {
            log.info(field + ": average" + " mean: " + (100-mean) + " (std: " + std + ")");
        }
        else {
            log.info(field + ": average" + " mean: " + (mean) + " (std: " + std + ")");
        }
    }

    private long getDurationInSeconds(WorkflowDTO workflowDTO, WorkflowDTO lastExecutedWorkflowRun1) {
        long start = workflowDTO.getArrivedAt().getTime();
        long end = lastExecutedWorkflowRun1.getFinishedAt().getTime();
        return TimeUnit.MILLISECONDS.toMinutes(end - start);
    }


    public double[] penalty(List<WorkflowDTO> workflowsRun, String executionCount) throws SQLException {
        double[] results = new double[2];
        double penalityPoints = 0;
        double missedDeadlines = 0;
        double totalDeadlines = 0;
        double overallOverallDuration = 0;
        double percentage;
        for (WorkflowDTO wf : workflowsRun) {
            totalDeadlines++;

            Double overallDuration = Double.valueOf(wf.getFinishedAt().getTime() - wf.getArrivedAt().getTime());
            Double timediff = Double.valueOf(wf.getFinishedAt().getTime() - wf.getDeadline().getTime());
            overallOverallDuration = overallOverallDuration + overallDuration;
            //  System.out.println("FinishedAt: " + wf.getFinishedAt().getTime() + "ArrivedAt: " + wf.getArrivedAt().getTime() + "Difference: " + timediff);
            if (timediff > 0) {
                penalityPoints += Math.ceil((timediff / overallDuration) * 10);
                missedDeadlines++;
            }
        }
        percentage = (100.0 / workflowsRun.size()) * missedDeadlines;
//        log.info("Missed deadlines of run " + executionCount + ": " + missedDeadlines + "/" + totalDeadlines + "(" + percentage + "%), Penalty points: " + penalityPoints + ", Overall Duration: " + overallOverallDuration);
        return new double[]{percentage, penalityPoints};
    }

}//end FirstExample