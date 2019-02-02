package at.ac.tuwien.infosys.viepepc;

import at.ac.tuwien.infosys.viepepc.database.entities.VMActionsDTO;
import at.ac.tuwien.infosys.viepepc.database.entities.WorkflowDTO;
import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.*;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.ColumnArrangement;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;
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
@Component("jFreeChartCreator")
public class JFreeChartCreator {

    public void writeAsPDF(JFreeChart chart, OutputStream out, int width, int height) {
//        try {
//
//            Document document = new Document(new Rectangle(width - 27, height - 11));
//            PdfWriter writer = PdfWriter.getInstance(document, out);
//            document.open();
//
//            PdfContentByte cb = writer.getDirectContent();
//            PdfTemplate pdfTemplate = cb.createTemplate(width, height);
//            Graphics2D g2d1 = new PdfGraphics2D(pdfTemplate, width, height);
//            Rectangle2D r2d1 = new Rectangle2D.Double(0, 0, width, height);
//            chart.draw(g2d1, r2d1);
//            g2d1.dispose();
//            cb.addTemplate(pdfTemplate, -13, -9);
//
//            document.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        try {

            Document document = new Document(new Rectangle(width, height));
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate pdfTemplate = cb.createTemplate(width, height);
            Graphics2D g2d1 = new PdfGraphics2D(pdfTemplate, width, height);
            Rectangle2D r2d1 = new Rectangle2D.Double(0, 0, width, height);
            chart.draw(g2d1, r2d1);
            g2d1.dispose();
            cb.addTemplate(pdfTemplate, 0, 0);

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

        final JFreeChart chart = ChartFactory.createTimeSeriesChart(null, "Time in Minutes", "# Leased CPU Cores", dataset1, true, false, false);

        chart.setBackgroundPaint(Color.white);
        chart.setBorderVisible(false);

        final XYPlot plot = chart.getXYPlot();

        createPlot(dataset3, maxDate, maxCoreAxisValue, coreAxisSteps, plot);

        Font defaultFont = new Font("Arial", Font.PLAIN, 22);


//        plot.getLegendItems().get(0).setLineStroke();

        LegendTitle legendTitle = chart.getLegend();
        LegendTitle legendTitleNew = new LegendTitle(plot, new ColumnArrangement(), new ColumnArrangement());
        legendTitleNew.setPosition(legendTitle.getPosition());
        legendTitleNew.setBackgroundPaint(legendTitle.getBackgroundPaint());
        legendTitleNew.setFrame(new BlockBorder(Color.black));
        legendTitleNew.setItemFont(defaultFont);






        XYTitleAnnotation ta = new XYTitleAnnotation(1, 1, legendTitleNew, RectangleAnchor.TOP_RIGHT);

        plot.addAnnotation(ta);
        chart.removeLegend();
        chart.setPadding(new RectangleInsets(1,-11,-7,-11));

        return chart;

    }

    protected XYPlot createPlot(XYDataset dataset3, Date maxDate, int maxCoreAxisValue, int coreAxisSteps, XYPlot plot) {
        plot.setBackgroundPaint(Color.white);

        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);

        Font defaultFont = plot.getDomainAxis().getLabelFont();
        defaultFont = new Font("Arial", Font.PLAIN, 22);


        {   // Process Axis
            plot.setDataset(1, dataset3);
            plot.mapDatasetToRangeAxis(1, 1);
            final ValueAxis axis2 = new NumberAxis("# Arrived Processe Requests");
            axis2.setLabelFont(defaultFont);
            axis2.setTickLabelFont(defaultFont);

            axis2.setLowerBound(0.0);
            axis2.setLowerMargin(0.0);
            axis2.setUpperBound(0.0);
            axis2.setUpperMargin(0.0);
            axis2.setLabelInsets(new RectangleInsets(0,0,0,0));

            plot.setRangeAxis(1, axis2);

            final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis(1);
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis.setAutoRangeIncludesZero(true);
            rangeAxis.setRange(0, 8);
            NumberTickUnit unit = new NumberTickUnit(2);
            rangeAxis.setTickUnit(unit);
        }

        {   // Time Axis
//            DateAxis axis = (DateAxis) plot.getDomainAxis();
            final DateAxis axis2 = new DateAxis("Time in Minutes");
            axis2.setLabelFont(defaultFont);
            axis2.setTickLabelFont(defaultFont);
            axis2.setLabelFont(defaultFont);
            axis2.setTickLabelFont(defaultFont);
            axis2.setTickLabelPaint(Color.black);

            axis2.setLabelPaint(Color.black);
            axis2.setDateFormatOverride(new CustomSimpleDateFormat("mm"));
            axis2.setMaximumDate(maxDate);
            axis2.setTickUnit(new DateTickUnit(DateTickUnitType.MINUTE, 20));

            axis2.setLabelInsets(new RectangleInsets(0,0,0,0));
            plot.setDomainAxis(axis2);

            final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesLinesVisible(0, false);
            renderer.setSeriesShapesVisible(0, true);
            renderer.setSeriesPaint(0, Color.red);
            renderer.setDefaultStroke(new BasicStroke(1.0f));
            renderer.setSeriesShape(0, ShapeUtils.createRegularCross(3f, 0.1f));

//            renderer.setLegendLine(ShapeUtils.createRegularCross(10f, 10f));
            renderer.setLegendShape(0, ShapeUtils.createRegularCross(3f, 0.1f));

            renderer.setDefaultLegendShape(ShapeUtils.createRegularCross(13f, 13f));

            plot.setRenderer(1, renderer);

        }

        {   // VM axis
            final ValueAxis axis2 = new NumberAxis("# Leased CPU Cores");
            axis2.setLabelFont(defaultFont);
            axis2.setTickLabelFont(defaultFont);

            axis2.setLowerBound(0.0);
            axis2.setLowerMargin(0.0);
            axis2.setUpperBound(0.0);
            axis2.setUpperMargin(0.0);
            axis2.setLabelInsets(new RectangleInsets(0,0,0,0));

            plot.setRangeAxis(0, axis2);
            final NumberAxis rangeAxis1 = (NumberAxis) plot.getRangeAxis(0);
            rangeAxis1.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis1.setAutoRangeIncludesZero(true);
            NumberTickUnit unit1 = new NumberTickUnit(coreAxisSteps);
            rangeAxis1.setTickUnit(unit1);
            rangeAxis1.setRange(0, maxCoreAxisValue);
            rangeAxis1.setLabelFont(defaultFont);
            rangeAxis1.setTickLabelFont(defaultFont);

            final XYStepRenderer renderer = new XYStepRenderer();
            renderer.setSeriesLinesVisible(0, true);
            renderer.setSeriesShapesVisible(0, false);

            renderer.setSeriesItemLabelPaint(0, Color.green);

            plot.setRenderer(0, renderer);

            XYItemRenderer rendererForDataset = plot.getRendererForDataset(plot.getDataset(0));
            rendererForDataset.setSeriesPaint(0, Color.blue);
            rendererForDataset.setSeriesStroke(0, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 1.0f, new float[]{1.0f}, 0.0f));

//            renderer.setLegendShape(0, ShapeUtils.createLineRegion());

            rendererForDataset.setSeriesPaint(1, Color.DARK_GRAY);
            rendererForDataset.setSeriesStroke(1, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{4.0f}, 2.0f));
//            renderer.setLegendShape(1, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{4.0f}, 2.0f).createStrokedShape(ShapeUtils.createRegularCross(10f, 10f)));


//            rendererForDataset.getLegendItems().get(0).setOutlinePaint(Color.red);
//            rendererForDataset.getLegendItems().get(0).setLinePaint(Color.green);
//            rendererForDataset.getLegendItems().get(0).setFillPaint(Color.yellow);
//            rendererForDataset.getLegendItems().get(0).setOutlineStroke(new BasicStroke(4.0f));
//            rendererForDataset.getLegendItems().get(1).setFillPaint(Color.DARK_GRAY);
//            rendererForDataset.getLegendItems().get(1).setLineStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{4.0f}, 2.0f));

//            renderer.getLegendItem(1,0).setFillPaint(Color.green);

        }


        return plot;
    }

    protected class CustomSimpleDateFormat extends SimpleDateFormat {
        public CustomSimpleDateFormat(String m) {
            super(m);
        }

        @Override
        public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
            StringBuffer format = super.format(date, toAppendTo, pos);
            GregorianCalendar greg = new GregorianCalendar();
            greg.setTime(date);

            int hour = greg.get(Calendar.HOUR_OF_DAY);
            int minute = greg.get(Calendar.MINUTE);
            int second = greg.get(Calendar.SECOND);
            int currentMinuteOfDay = ((hour - 1) * 60) + minute + second;
            format = new StringBuffer(String.valueOf(currentMinuteOfDay));

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

            RegularTimePeriod period = new Second(date);
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
//        final TimeSeries series1 = new TimeSeries("# Leased CPU Cores 1");
//        final TimeSeries series2 = new TimeSeries("# Leased CPU Cores 2");
//        final TimeSeries series3 = new TimeSeries("# Leased CPU Cores 3");

        VMActionsDTO first = null;
        VMActionsDTO last = null;

        first = getFirst(evaluation3, getFirst(evaluation2, getFirst(evaluation1, null)));
        last = getLast(evaluation3, getLast(evaluation2, getLast(evaluation1, null)));


        Calendar dateStart = new GregorianCalendar();
        dateStart.setTime(first.getDate());
        dateStart.set(Calendar.MILLISECOND, 0);

        Calendar dateEnd = new GregorianCalendar();
        dateEnd.setTime(last.getDate());

        Map<Integer, List<VMActionsDTO>> values = new TreeMap<>();
        long maxSeconds = TimeUnit.MILLISECONDS.toSeconds(dateEnd.getTimeInMillis() - dateStart.getTimeInMillis());

        for (int i = 0; i <= maxSeconds; i++) {
            values.put(i, new ArrayList<>());
        }

        for (VMActionsDTO containerActionsDTO : evaluation1) {
            int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(containerActionsDTO.getDate().getTime() - dateStart.getTimeInMillis());
            if (values.get(seconds) != null) {
                values.get(seconds).add(containerActionsDTO);
            }
        }
        for (VMActionsDTO containerActionsDTO : evaluation2) {
            int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(containerActionsDTO.getDate().getTime() - dateStart.getTimeInMillis());
            if (values.get(seconds) != null) {
                values.get(seconds).add(containerActionsDTO);
            }
        }
        for (VMActionsDTO containerActionsDTO : evaluation3) {
            int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(containerActionsDTO.getDate().getTime() - dateStart.getTimeInMillis());
            if (values.get(seconds) != null) {
                values.get(seconds).add(containerActionsDTO);
            }
        }


        double lastAverage = 0;
        for (Integer second : values.keySet()) {
//            String output = "" + second + " ";
            double sum = 0;
            Date date = null;
            for (VMActionsDTO dto : values.get(second)) {
//                output += " " + dto.getCoreAmount();
                sum = sum + dto.getCoreAmount();
                date = dto.getDate();
            }
//            System.out.println(output + " avg: " + sum / 3.0);

            //if(date != null) {
                RegularTimePeriod period = new Millisecond(date);
                TimeSeriesDataItem timeSeriesDataItem = new TimeSeriesDataItem(period, sum / 3.0);
                averages.add(timeSeriesDataItem);
            //}
        }


//        for (VMActionsDTO containerActionsDTO : evaluation1) {
//            RegularTimePeriod period = new Second(containerActionsDTO.getDate());
//            TimeSeriesDataItem timeSeriesDataItem = new TimeSeriesDataItem(period, containerActionsDTO.getCoreAmount());
//            series1.add(timeSeriesDataItem);
//        }
//        for (VMActionsDTO containerActionsDTO : evaluation2) {
//            RegularTimePeriod period = new Second(containerActionsDTO.getDate());
//            TimeSeriesDataItem timeSeriesDataItem = new TimeSeriesDataItem(period, containerActionsDTO.getCoreAmount());
//            series2.add(timeSeriesDataItem);
//        }
//        for (VMActionsDTO containerActionsDTO : evaluation3) {
//            RegularTimePeriod period = new Second(containerActionsDTO.getDate());
//            TimeSeriesDataItem timeSeriesDataItem = new TimeSeriesDataItem(period, containerActionsDTO.getCoreAmount());
//            series3.add(timeSeriesDataItem);
//        }

        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(averages);
//        dataset.addSeries(series1);
//        dataset.addSeries(series2);
//        dataset.addSeries(series3);
        return dataset;
    }


    private VMActionsDTO getFirst(List<VMActionsDTO> evaluation1, VMActionsDTO first) {
        for (VMActionsDTO containerActionsDTO : evaluation1) {
            if (first == null) {
                first = containerActionsDTO;
            } else if (first.getDate().after(containerActionsDTO.getDate())) {
                first = containerActionsDTO;
            }
        }
        return first;
    }

    private VMActionsDTO getLast(List<VMActionsDTO> evaluation, VMActionsDTO last) {
        for (VMActionsDTO containerActionsDTO : evaluation) {
            if (last == null) {
                last = containerActionsDTO;
            } else if (last.getDate().before(containerActionsDTO.getDate())) {
                last = containerActionsDTO;
            }
        }
        return last;
    }


}
