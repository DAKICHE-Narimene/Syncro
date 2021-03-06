/**
 * Copyright (c) 2004-2006 Regents of the University of California. See
 * "license-prefuse.txt" for licensing terms.
 */
package prefuse.demos;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.JComponent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.layout.Layout;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.ControlAdapter;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.data.Graph;
import prefuse.data.Node;
import static prefuse.demos.AggregateDragControl.setFixed;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.render.PolygonRenderer;
import prefuse.render.Renderer;
import prefuse.util.ColorLib;
import prefuse.util.GraphicsLib;
import prefuse.visual.AggregateItem;
import prefuse.visual.AggregateTable;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;

/**
 * Demo application showcasing the use of AggregateItems to visualize groupings
 * of nodes with in a graph visualization.
 *
 * This class uses the AggregateLayout class to compute bounding polygons for
 * each aggregate and the AggregateDragControl to enable drags of both nodes and
 * node aggregates.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class AggregateDemo extends Display {

    public static final String GRAPH = "graph";
    public static final String NODES = "graph.nodes";
    public static final String EDGES = "graph.edges";
    public static final String AGGR = "aggregates";

    public AggregateDemo(Graph g, String label) {
        super(new Visualization());
        initDataGroups(g);

//        DefaultRendererFactory drf = (DefaultRendererFactory) m_vis.getRendererFactory();
//		((LabelRenderer) drf.getDefaultRenderer()).setTextField("name");
        LabelRenderer tr = new LabelRenderer();

        tr.setRoundedCorner(8, 8);
        m_vis.setRendererFactory(new DefaultRendererFactory(tr));
        DefaultRendererFactory drf = (DefaultRendererFactory) m_vis.getRendererFactory();
        ((LabelRenderer) drf.getDefaultRenderer()).setTextField(label);

        // set up the renderers
        // draw the nodes as basic shapes
        // Renderer nodeR = new ShapeRenderer(20);
        // draw aggregates as polygons with curved edges
        Renderer polyR = new PolygonRenderer(Constants.POLY_TYPE_CURVE);
        ((PolygonRenderer) polyR).setCurveSlack(0.15f);

        //DefaultRendererFactory drf = new DefaultRendererFactory();
        // drf.setDefaultRenderer(nodeR);
        drf.add("ingroup('aggregates')", polyR);
      //  m_vis.setRendererFactory(drf);

        // set up the visual operators
        // first set up all the color actions
       /* ColorAction nStroke = new ColorAction(NODES, VisualItem.STROKECOLOR);
         nStroke.setDefaultColor(ColorLib.gray(100));
         nStroke.add("_hover", ColorLib.gray(50));
        
         ColorAction nFill = new ColorAction(NODES, VisualItem.FILLCOLOR);
         nFill.setDefaultColor(ColorLib.gray(255));
         nFill.add("_hover", ColorLib.gray(200));
        
         ColorAction nEdges = new ColorAction(EDGES, VisualItem.STROKECOLOR);
         nEdges.setDefaultColor(ColorLib.gray(100));
        

        
         ColorAction aStroke = new ColorAction(AGGR, VisualItem.STROKECOLOR);
         aStroke.setDefaultColor(ColorLib.gray(200));
         aStroke.add("_hover", ColorLib.rgb(255,100,100));*/
        int[] palette = new int[]{
            ColorLib.rgba(255, 200, 200, 150),
            ColorLib.rgba(200, 255, 200, 150),
            ColorLib.rgba(200, 200, 255, 150),
            ColorLib.rgba(200, 200, 255, 150),
            ColorLib.rgba(126, 23, 230, 150),
            ColorLib.rgba(188, 23, 230, 150),
            ColorLib.rgba(126, 230, 23, 150),
            ColorLib.rgba(200, 200, 255, 150),
            ColorLib.rgba(23, 230, 106, 150),
            ColorLib.rgba(23, 230, 168, 150),
            ColorLib.rgba(23, 168, 230, 150),
            ColorLib.rgba(23, 44, 230, 150),
            ColorLib.rgba(23, 230, 44, 150),
            ColorLib.rgba(230, 23, 23, 150),
            ColorLib.rgba(230, 209, 23, 150),
            ColorLib.rgba(200, 200, 0, 150)
        };
        Random rnd = ThreadLocalRandom.current();
        for (int i = palette.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            int a = palette[index];
            palette[index] = palette[i];
            palette[i] = a;
        }
        ColorAction aFill = new DataColorAction(AGGR, "id",
                Constants.NOMINAL, VisualItem.FILLCOLOR, palette);

        // bundle the color actions
        ActionList colors = new ActionList();
        /* colors.add(nStroke);
         colors.add(nFill);
         colors.add(nEdges);
         colors.add(aStroke);*/
        colors.add(aFill);

        /*int hops = 30;
         final GraphDistanceFilter filter = new GraphDistanceFilter(GRAPH, hops);*/
        ColorAction fill = new ColorAction(NODES, VisualItem.FILLCOLOR, ColorLib.gray(255)); //NODES, VisualItem.FILLCOLOR, ColorLib.rgb(200, 200, 255)
        fill.add(VisualItem.FIXED, ColorLib.rgb(255, 100, 100));
        fill.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255, 200, 125));

        ActionList draw = new ActionList();
        colors.add(fill);
        colors.add(new ColorAction(NODES, VisualItem.STROKECOLOR, ColorLib.gray(100)));
        colors.add(new ColorAction(NODES, VisualItem.FILLCOLOR, ColorLib.gray(255)));
        colors.add(new ColorAction(NODES, VisualItem.TEXTCOLOR, ColorLib.rgb(0, 0, 0)));
        //colors.add(new ColorAction(EDGES, VisualItem.FILLCOLOR, ColorLib.gray(200)));
        colors.add(new ColorAction(EDGES, VisualItem.STROKECOLOR, ColorLib.gray(100)));

        // now create the main layout routine
        ActionList layout = new ActionList(Activity.INFINITY);
        layout.add(colors);
        layout.add(new ForceDirectedLayout(GRAPH, true));
        layout.add(new AggregateLayout(AGGR));
        layout.add(new RepaintAction());
        m_vis.putAction("layout", layout);

        // set up the display
        setSize(310, 310);
        pan(155, 155);
        setHighQuality(true);
        addControlListener(new AggregateDragControl());
        addControlListener(new ZoomControl());
        addControlListener(new PanControl());
        addControlListener(new WheelZoomControl());

        // set things running
        m_vis.run("layout");
    }

    private void initDataGroups(Graph g) {
        VisualGraph vg = m_vis.addGraph(GRAPH, g);
        m_vis.setInteractive(EDGES, null, false);
        m_vis.setValue(NODES, null, VisualItem.SHAPE,
                new Integer(Constants.SHAPE_ELLIPSE));

        AggregateTable at = m_vis.addAggregates(AGGR);
        at.addColumn(VisualItem.POLYGON, float[].class);
        at.addColumn("id", int.class);

		// add nodes to aggregates
        // create an aggregate for each 3-clique of nodes
        Iterator nodes = vg.nodes();
        //for (int i = 0; i < 4; ++i) {
        AggregateItem aitem = (AggregateItem) at.addItem();
        aitem.setInt("id", 1);
        //System.out.println("Data: " + i);
        for (int j = 0; j < vg.getNodeCount(); ++j) {
            aitem.addItem((VisualItem) nodes.next());
        }
    }

    public static void main(String[] argv) {

        Graph g = new Graph();
        String label = "name";
        g.addColumn(label, String.class);
        for (int i = 0; i < 3; ++i) {
            Node n1 = g.addNode();
            n1.setString(label, "peofkgozfzpigp pzps");
            Node n2 = g.addNode();
            n2.setString(label, "1");
            Node n3 = g.addNode();
            n3.setString(label, "1");
            g.addEdge(n1, n2);
            g.addEdge(n1, n3);
            g.addEdge(n2, n3);
        }
        g.addEdge(0, 3);
        g.addEdge(3, 6);
        g.addEdge(6, 0);
        JFrame frame = demo(g, label);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public static JFrame demo(Graph g, String label) {

        AggregateDemo ad = new AggregateDemo(g, label);
        JFrame frame = new JFrame("p r e f u s e  |  a g g r e g a t e d");
        frame.getContentPane().add(ad);
        frame.pack();
        return frame;
    }

    public static JComponent demoComp(Graph g, String label) {

        AggregateDemo ad = new AggregateDemo(g, label);
//        JFrame frame = new JFrame("p r e f u s e  |  a g g r e g a t e d");
//        frame.getContentPane().add(ad);
//        frame.pack();
        return ad;
    }

} // end of class AggregateDemo

/**
 * Layout algorithm that computes a convex hull surrounding aggregate items and
 * saves it in the "_polygon" field.
 */
class AggregateLayout extends Layout {

    private int m_margin = 5; // convex hull pixel margin
    private double[] m_pts;   // buffer for computing convex hulls

    public AggregateLayout(String aggrGroup) {
        super(aggrGroup);
    }

    /**
     * @see
     * edu.berkeley.guir.prefuse.action.Action#run(edu.berkeley.guir.prefuse.ItemRegistry,
     * double)
     */
    public void run(double frac) {

        AggregateTable aggr = (AggregateTable) m_vis.getGroup(m_group);
        // do we have any  to process?
        int num = aggr.getTupleCount();
        if (num == 0) {
            return;
        }

        // update buffers
        int maxsz = 0;
        for (Iterator aggrs = aggr.tuples(); aggrs.hasNext();) {
            maxsz = Math.max(maxsz, 4 * 2
                    * ((AggregateItem) aggrs.next()).getAggregateSize());
        }
        if (m_pts == null || maxsz > m_pts.length) {
            m_pts = new double[maxsz];
        }

        // compute and assign convex hull for each aggregate
        Iterator aggrs = m_vis.visibleItems(m_group);
        while (aggrs.hasNext()) {
            AggregateItem aitem = (AggregateItem) aggrs.next();

            int idx = 0;
            if (aitem.getAggregateSize() == 0) {
                continue;
            }
            VisualItem item = null;
            Iterator iter = aitem.items();
            while (iter.hasNext()) {
                item = (VisualItem) iter.next();
                if (item.isVisible()) {
                    addPoint(m_pts, idx, item, m_margin);
                    idx += 2 * 4;
                }
            }
            // if no aggregates are visible, do nothing
            if (idx == 0) {
                continue;
            }

            // compute convex hull
            double[] nhull = GraphicsLib.convexHull(m_pts, idx);

            // prepare viz attribute array
            float[] fhull = (float[]) aitem.get(VisualItem.POLYGON);
            if (fhull == null || fhull.length < nhull.length) {
                fhull = new float[nhull.length];
            } else if (fhull.length > nhull.length) {
                fhull[nhull.length] = Float.NaN;
            }

            // copy hull values
            for (int j = 0; j < nhull.length; j++) {
                fhull[j] = (float) nhull[j];
            }
            aitem.set(VisualItem.POLYGON, fhull);
            aitem.setValidated(false); // force invalidation
        }
    }

    private static void addPoint(double[] pts, int idx,
            VisualItem item, int growth) {
        Rectangle2D b = item.getBounds();
        double minX = (b.getMinX()) - growth, minY = (b.getMinY()) - growth;
        double maxX = (b.getMaxX()) + growth, maxY = (b.getMaxY()) + growth;
        pts[idx] = minX;
        pts[idx + 1] = minY;
        pts[idx + 2] = minX;
        pts[idx + 3] = maxY;
        pts[idx + 4] = maxX;
        pts[idx + 5] = minY;
        pts[idx + 6] = maxX;
        pts[idx + 7] = maxY;
    }

} // end of class AggregateLayout

/**
 * Interactive drag control that is "aggregate-aware"
 */
class AggregateDragControl extends ControlAdapter {

    private VisualItem activeItem;
    protected Point2D down = new Point2D.Double();
    protected Point2D temp = new Point2D.Double();
    protected boolean dragged;

    /**
     * Creates a new drag control that issues repaint requests as an item is
     * dragged.
     */
    public AggregateDragControl() {
    }

    /**
     * @see prefuse.controls.Control#itemEntered(prefuse.visual.VisualItem,
     * java.awt.event.MouseEvent)
     */
    public void itemEntered(VisualItem item, MouseEvent e) {
        Display d = (Display) e.getSource();
        d.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        activeItem = item;
        if (!(item instanceof AggregateItem)) {
            setFixed(item, true);
        }
    }

    /**
     * @see prefuse.controls.Control#itemExited(prefuse.visual.VisualItem,
     * java.awt.event.MouseEvent)
     */
    public void itemExited(VisualItem item, MouseEvent e) {
        if (activeItem == item) {
            activeItem = null;
            setFixed(item, false);
        }
        Display d = (Display) e.getSource();
        d.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * @see prefuse.controls.Control#itemPressed(prefuse.visual.VisualItem,
     * java.awt.event.MouseEvent)
     */
    public void itemPressed(VisualItem item, MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        dragged = false;
        Display d = (Display) e.getComponent();
        d.getAbsoluteCoordinate(e.getPoint(), down);
        if (item instanceof AggregateItem) {
            setFixed(item, true);
        }
    }

    /**
     * @see prefuse.controls.Control#itemReleased(prefuse.visual.VisualItem,
     * java.awt.event.MouseEvent)
     */
    public void itemReleased(VisualItem item, MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        if (dragged) {
            activeItem = null;
            setFixed(item, false);
            dragged = false;
        }
    }

    /**
     * @see prefuse.controls.Control#itemDragged(prefuse.visual.VisualItem,
     * java.awt.event.MouseEvent)
     */
    public void itemDragged(VisualItem item, MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        dragged = true;
        Display d = (Display) e.getComponent();
        d.getAbsoluteCoordinate(e.getPoint(), temp);
        double dx = temp.getX() - down.getX();
        double dy = temp.getY() - down.getY();

        move(item, dx, dy);

        down.setLocation(temp);
    }

    protected static void setFixed(VisualItem item, boolean fixed) {
        if (item instanceof AggregateItem) {
            Iterator items = ((AggregateItem) item).items();
            while (items.hasNext()) {
                setFixed((VisualItem) items.next(), fixed);
            }
        } else {
            item.setFixed(fixed);
        }
    }

    protected static void move(VisualItem item, double dx, double dy) {
        if (item instanceof AggregateItem) {
            Iterator items = ((AggregateItem) item).items();
            while (items.hasNext()) {
                move((VisualItem) items.next(), dx, dy);
            }
        } else {
            double x = item.getX();
            double y = item.getY();
            item.setStartX(x);
            item.setStartY(y);
            item.setX(x + dx);
            item.setY(y + dy);
            item.setEndX(x + dx);
            item.setEndY(y + dy);
        }
    }

} // end of class AggregateDragControl
