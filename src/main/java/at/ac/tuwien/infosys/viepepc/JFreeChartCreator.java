package at.ac.tuwien.infosys.viepepc;

import at.ac.tuwien.infosys.viepepc.database.entities.VMActionsDTO;
import at.ac.tuwien.infosys.viepepc.database.entities.WorkflowDTO;
import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;
import org.jfree.util.ShapeUtilities;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Philipp Hoenisch on 9/1/14.
 */
@Component
public class JFreeChartCreator {

    public void writeAsPDF(JFreeChart chart, OutputStream out, int width, int height) {
        try {
            Rectangle pagesize = new Rectangle(width, height);
            Document document = new Document(pagesize, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate tp = cb.createTemplate(width, height);
            Graphics2D g2 = tp.createGraphics(width, height, new DefaultFontMapper());
            Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
            chart.draw(g2, r2D);
            g2.dispose();
            cb.addTemplate(tp, 0, 0);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a chart.
     *
     * @param name
     * @param dataset1 the data for the chart.
     * @param maxDate
     * @return a chart.
     */
    protected JFreeChart createChart(String name, final XYDataset dataset3, final XYDataset dataset1, Date maxDate, int maxCoreAxisValue, int coreAxisSteps) {

        final JFreeChart chart = ChartFactory.createTimeSeriesChart(
                name,      // chart title
                "Time in Minutes",                      // x axis label
                "# Leased CPU Cores",                      // y axis label
                dataset1,                  // data
//                PlotOrientation.VERTICAL,
                true,                     // include legend
                true,                     // tooltips
                false                     // urls
        );


        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.setBackgroundPaint(Color.white);
        chart.setBorderVisible(true);

        // get a reference to the plot for further customisation...
        final XYPlot plot = chart.getXYPlot();


        plot.setBackgroundPaint(Color.white);
//        plot.setRangeGridlinePaint(Color.white);

        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        chart.setBorderVisible(false);

        { // Process axis
            // change the auto tick unit selection to integer units only...
            plot.setDataset(1, dataset3);
            plot.mapDatasetToRangeAxis(1, 1);
            final ValueAxis axis2 = new NumberAxis("# Arrived Processes");
            plot.setRangeAxis(1, axis2);

            final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis(1);
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis.setAutoRangeIncludesZero(true);
            rangeAxis.setRange(0, 10);
            NumberTickUnit unit = new NumberTickUnit(1);
            rangeAxis.setTickUnit(unit);

            DateAxis axis = (DateAxis) plot.getDomainAxis();
            DateFormat dateformat = new CustomSimpleDateFormat("mm");
            axis.setDateFormatOverride(dateformat);//new SimpleDateFormat("m"));
//            axis.setDateFormatOverride(new SimpleDateFormat("mm"));
            axis.setTickUnit(new DateTickUnit(DateTickUnitType.MINUTE, 5, DateTickUnitType.HOUR, 1, dateformat));
//            axis.setAutoRangeMinimumSize(30);
            axis.setMaximumDate(maxDate);

            final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesLinesVisible(0, true);
            renderer.setSeriesShapesVisible(0, true);
            plot.setRenderer(1, renderer);
            plot.getRendererForDataset(plot.getDataset(1)).setSeriesPaint(0, Color.red);
            plot.getRendererForDataset(plot.getDataset(1)).setBaseStroke(new BasicStroke(2f));
            plot.getRendererForDataset(plot.getDataset(1)).setSeriesShape(0, ShapeUtilities.createRegularCross(2f, 1));



        }

        {// VM axis

            final ValueAxis axis2 = new NumberAxis("# Leased CPU Cores");
            plot.setRangeAxis(0, axis2);
            final NumberAxis rangeAxis1 = (NumberAxis) plot.getRangeAxis(0);
            rangeAxis1.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis1.setAutoRangeIncludesZero(true);
            NumberTickUnit unit1 = new NumberTickUnit(coreAxisSteps);
            rangeAxis1.setTickUnit(unit1);
            rangeAxis1.setRange(0, maxCoreAxisValue);

            final XYStepRenderer renderer = new XYStepRenderer();
            renderer.setSeriesLinesVisible(0, true);
            renderer.setSeriesShapesVisible(0, false);
            plot.setRenderer(0, renderer);

            XYItemRenderer rendererForDataset = plot.getRendererForDataset(plot.getDataset(0));
            rendererForDataset.setSeriesPaint(0, Color.blue);
            rendererForDataset.setSeriesStroke(0, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{3.0f, 3.0f}, 3.0f));
            rendererForDataset.setSeriesPaint(1, Color.DARK_GRAY);
            rendererForDataset.setSeriesStroke(1, new BasicStroke(2f));
            rendererForDataset.setSeriesPaint(2, Color.yellow);
            rendererForDataset.setSeriesPaint(3, Color.orange);
        }

        return chart;

    }

    private class CustomSimpleDateFormat extends SimpleDateFormat {
        public CustomSimpleDateFormat(String m) {
            super(m);
        }

        @Override
        public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
            StringBuffer format = super.format(date, toAppendTo, pos);
            GregorianCalendar greg = new GregorianCalendar();
            greg.setTime(date);
            int i = greg.get(Calendar.HOUR_OF_DAY);
            if (i > 1) {
                int ne = greg.get(Calendar.MINUTE) + ((i - 1) * 60);
                String ne1 = ne + "";
                format.setCharAt(0, ne1.charAt(0));
                format.setCharAt(1, ne1.charAt(1));
            }

            return format;
        }

        @Override
        public Date parse(String text, ParsePosition pos) {
            return super.parse(text, pos);
        }

    }

    public TimeSeriesCollection createArrivalDataSet(List<WorkflowDTO> results) {
        final TimeSeries series1 = new TimeSeries("Process Arrivals");
        SortedMap<Date, List<WorkflowDTO>> arrivalSorted = new TreeMap<>();
        for (WorkflowDTO result : results) {
            Date arrivedAt = result.getArrivedAt();
            List<WorkflowDTO> workflowDTOs = arrivalSorted.get(arrivedAt);
            if (workflowDTOs == null) {
                workflowDTOs = new ArrayList<>();
            }
            workflowDTOs.add(result);
            arrivalSorted.put(arrivedAt, workflowDTOs);
        }
        int x = 0;
        Date min = null;
        for (Date date : arrivalSorted.keySet()) {
            if (min == null) {
                min = date;
            }
            List<WorkflowDTO> workflowDTOs = arrivalSorted.get(date);
            date = new Date(date.getTime() - min.getTime());

            RegularTimePeriod period = new Minute(date);
            TimeSeriesDataItem timeSeriesDataItem = new TimeSeriesDataItem(period, workflowDTOs.size());
            series1.addOrUpdate(timeSeriesDataItem);
            x++;
        }

        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series1);

        return dataset;

    }

    public TimeSeriesCollection createVMDataSet(String setName, List<VMActionsDTO> evaluation1, List<VMActionsDTO> evaluation2, List<VMActionsDTO> evaluation3) {
        final TimeSeries averages = new TimeSeries(setName);
        final TimeSeries series1 = new TimeSeries("# Leased CPU Cores 1");
        final TimeSeries series2 = new TimeSeries("# Leased CPU Cores 2");
        final TimeSeries series3 = new TimeSeries("# Leased CPU Cores 3");

        VMActionsDTO first = null;
        VMActionsDTO last = null;

        first = getFirst(evaluation3, getFirst(evaluation2, getFirst(evaluation1, null)));
        last = getLast(evaluation3, getLast(evaluation2, getLast(evaluation1, null)));


        Calendar dateStart = new GregorianCalendar();
        dateStart.setTime(first.getDate());
        dateStart.set(Calendar.MINUTE, 0);

        Calendar dateEnd = new GregorianCalendar();
        dateEnd.setTime(last.getDate());

        Map<Integer, List<VMActionsDTO>> values = new TreeMap<>();
        long maxMinutes = TimeUnit.MILLISECONDS.toMinutes(dateEnd.getTimeInMillis() - dateStart.getTimeInMillis());

//        maxMinutes = (long) (Math.ceil(maxMinutes / 5.0) * 5);

        for (int i = 0; i <= maxMinutes; i++) {
            values.put(i, new ArrayList<VMActionsDTO>());
        }

        for (VMActionsDTO vmActionsDTO : evaluation1) {
            int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsDTO.getDate().getTime() - dateStart.getTimeInMillis());
            if (values.get(minutes) != null) {
                values.get(minutes).add(vmActionsDTO);
            }
        }
        for (VMActionsDTO vmActionsDTO : evaluation2) {
            int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsDTO.getDate().getTime() - dateStart.getTimeInMillis());
            if (values.get(minutes) != null) {
                values.get(minutes).add(vmActionsDTO);
            }
        }
        for (VMActionsDTO vmActionsDTO : evaluation3) {
            int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(vmActionsDTO.getDate().getTime() - dateStart.getTimeInMillis());
            if (values.get(minutes) != null) {
                values.get(minutes).add(vmActionsDTO);
            }
        }


        double lastAverage = 0;
        for (Integer minute : values.keySet()) {
            String output = "" + minute + " ";
            double sum = 0;
            Date date = null;
            for (VMActionsDTO dto : values.get(minute)) {
                output += " " + dto.getCoreAmount();
                sum += dto.getCoreAmount();
                date = dto.getDate();
            }
//            System.out.println(output + " avg: " + sum / 3.0);

            RegularTimePeriod period = new Millisecond(date);
            TimeSeriesDataItem timeSeriesDataItem = new TimeSeriesDataItem(period, sum / 3.0);
            averages.add(timeSeriesDataItem);
        }


        for (VMActionsDTO vmActionsDTO : evaluation1) {
            RegularTimePeriod period = new Minute(vmActionsDTO.getDate());
            TimeSeriesDataItem timeSeriesDataItem = new TimeSeriesDataItem(period, vmActionsDTO.getCoreAmount());
            series1.add(timeSeriesDataItem);
        }
        for (VMActionsDTO vmActionsDTO : evaluation2) {
            RegularTimePeriod period = new Minute(vmActionsDTO.getDate());
            TimeSeriesDataItem timeSeriesDataItem = new TimeSeriesDataItem(period, vmActionsDTO.getCoreAmount());
            series2.add(timeSeriesDataItem);
        }
        for (VMActionsDTO vmActionsDTO : evaluation3) {
            RegularTimePeriod period = new Minute(vmActionsDTO.getDate());
            TimeSeriesDataItem timeSeriesDataItem = new TimeSeriesDataItem(period, vmActionsDTO.getCoreAmount());
            series3.add(timeSeriesDataItem);
        }

        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(averages);
//        dataset.addSeries(series1);
//        dataset.addSeries(series2);
//        dataset.addSeries(series3);
        return dataset;
    }


    private VMActionsDTO getFirst(List<VMActionsDTO> evaluation1, VMActionsDTO first) {
        for (VMActionsDTO vmActionsDTO : evaluation1) {
            if (first == null) {
                first = vmActionsDTO;
            } else if (first.getDate().after(vmActionsDTO.getDate())) {
                first = vmActionsDTO;
            }
        }
        return first;
    }

    private VMActionsDTO getLast(List<VMActionsDTO> evaluation, VMActionsDTO last) {
        for (VMActionsDTO vmActionsDTO : evaluation) {
            if (last == null) {
                last = vmActionsDTO;
            } else if (last.getDate().before(vmActionsDTO.getDate())) {
                last = vmActionsDTO;
            }
        }
        return last;
    }


}
