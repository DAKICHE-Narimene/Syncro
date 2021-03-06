/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communityDetection.ExternMethods;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.implementations.SingleGraph;

/**
 *
 * @author HADJER
  * 
 * This class executes the .jar of the community detection method CONGA. 
 * The method is not ours, it belongs to its developper
 * For further information, please refer to the application help file
 */
public class CONGA extends CommunityMiner {

    private BufferedReader error;
    private BufferedReader op;
    private int exitVal;
    String jarFilePath = "\".\\LibDetection\\CONGA\\conga.jar\"";

    public LinkedList<Graph> findCommunities2(String filePath, int nbclusters) {
        // Arguments

        String filename = DetectionUtils.getfileName(filePath);
        LinkedList<Graph> communities = new LinkedList<>();

        String newFilePath=".\\LibDetection\\CONGA\\" + filename + ".txt";
        try { 
            File source= new File(filePath);
            File dest= new File(newFilePath);
            Files.copy(source.toPath(), dest.toPath(),REPLACE_EXISTING);

        } catch (IOException e) { 
            e.printStackTrace(); 
        }
        
        final List<String> actualArgs = new ArrayList<String>();
        actualArgs.add(0, "java");
        actualArgs.add(1, "-cp");
        actualArgs.add(2, jarFilePath);
        actualArgs.add(3, "CONGA");
        actualArgs.add(4, "\"" + newFilePath + "\"");
        actualArgs.add(5, "-e");
        actualArgs.add(6, "-r");
        actualArgs.add(7, "-mem");
        actualArgs.add(8, "-n");
        actualArgs.add(9, "" + nbclusters);

        //actualArgs.addAll(args);
        try {
            String line;
            Process p = Runtime.getRuntime().exec(actualArgs.toArray(new String[0]));
            BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = bri.readLine()) != null) {
                //System.out.println(line);
            }
            bri.close();
            while ((line = bre.readLine()) != null) {
                //System.out.println(line);
            }
            bre.close();
            p.waitFor();
            this.exitVal = p.exitValue();
            if (this.exitVal != 0) {
                throw new IOException("Failed to execure jar, " + this.getExecutionLog());
            } else {
                System.out.println("Done.");

                File f = new File("clusters-" + filename + ".txt");
                FileInputStream fis = new FileInputStream(f);
                //Construct BufferedReader from InputStreamReader
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));

                communities = new LinkedList<>();
                line = null;
                int nbcomm = 0;
                while ((line = br.readLine()) != null) {
                    System.out.println("comm:" + nbcomm + " == " + line);
                    String[] nodes = line.split(" ");
                    System.out.println("The number of nodes is: " + nodes.length);
                    communities.add(new SingleGraph(""));
                    for (String nodeId : nodes) {
                        if ((communities.get(nbcomm).getNode(nodeId)) == null) {
                            communities.get(nbcomm).addNode(nodeId);
                        }
                    }
                    nbcomm++;
                    //communities.add(new Community(new LinkedList(Arrays.asList(nodes)),null));
                }
                br.close();
                f.delete();

                //add the edges for each community
                f = new File(newFilePath);
                fis = new FileInputStream(f);
                //Construct BufferedReader from InputStreamReader
                br = new BufferedReader(new InputStreamReader(fis));

                line = null;
                //Read the file line by line and affect the edge to the community
                while ((line = br.readLine()) != null) {
                    System.out.println("line: == " + line);
                    String[] nodes = line./*split(" +");*/replaceAll("(^\\s+|\\s+$)", "").split("\\s+");
                    System.out.println("The number of nodes is: " + nodes.length);
                    String nodeId0 = new String(nodes[0]);
                    String nodeId1 = new String(nodes[1]);

                    //Search for the community that contains it
                    for (Graph com : communities) {
                        if (((com.getNode(nodeId0)) != null) && ((com.getNode(nodeId1)) != null)) {
                            try {
                                System.out.println("node affected==" + nodeId0 + ";" + nodeId1);
                                com.addEdge(nodeId0 + ";" + nodeId1, nodeId0, nodeId1);
                            } catch (EdgeRejectedException | IdAlreadyInUseException e) {
                                System.out.println("node affected==" + nodeId0 + ";" + nodeId1 + "rejected");
                            }
                        }
                    }
                }
                //add the edges for each community
                br.close();
                f.delete();
            }

        } catch (Exception err) {
            err.printStackTrace();
        }
        //Read the file and extract communities

        return communities;
    }

    public String getExecutionLog() {
        String error = "";
        String line;
        try {
            while ((line = this.error.readLine()) != null) {
                error = error + "\n" + line;
            }
        } catch (final IOException e) {
        }
        String output = "";
        try {
            while ((line = this.op.readLine()) != null) {
                output = output + "\n" + line;
            }
        } catch (final IOException e) {
        }
        try {
            this.error.close();
            this.op.close();
        } catch (final IOException e) {
        }
        return "exitVal: " + this.exitVal + ", error: " + error + ", output: " + output;
    }

    @Override
    public String getName() {
        return "CONGA";
    }

    @Override
    public String getShortName() {
        return "CONGA";
    }

    @Override
    public LinkedList<Graph> findCommunities(String filePath) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
