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
 * This class executes the .jar of the community detection method CONCLUDE. 
 * The method is not ours, it belongs to its developper
 * For further information, please refer to the application help file
 */
public class CONCLUDE extends CommunityMiner {

    private BufferedReader error;
    private BufferedReader op;
    private int exitVal;
    String jarFilePath = "\".\\LibDetection\\CONCLUDE\\CONCLUDE.jar\"";


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
        // Arguments

        String filename = DetectionUtils.getfileName(filePath);
        /*
        try { 
            Files.copy(source, destination); 
            // Il est également possible de spécifier des options de copie. 
            // Ici : écrase le fichier destination s'il existe et copie les attributs de la source sur la destination.  
           //Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES); 
        } catch (IOException e) { 
            e.printStackTrace(); 
        } */
        String newFilePath=".\\LibDetection\\CONCLUDE\\graphFile_" + filename + ".txt";
        try { 
            File source= new File(filePath);
            File dest= new File(newFilePath);
            Files.copy(source.toPath(), dest.toPath(),REPLACE_EXISTING);

        } catch (IOException e) { 
            e.printStackTrace(); 
        }
        
        
        LinkedList<Graph> communities = new LinkedList<>();

        final List<String> actualArgs = new ArrayList<String>();
        actualArgs.add(0, "java");
        actualArgs.add(1, "-jar");
        actualArgs.add(2, jarFilePath);
        actualArgs.add(3, "\"" + newFilePath + "\"");
        actualArgs.add(4, "\".\\LibDetection\\CONCLUDE\\clusters-" + "graphFile_" + filename + ".txt\"");//output file
        actualArgs.add(5, " ");//Delimiter

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
                throw new IOException("Failed to execure jar, ");// + this.getExecutionLog());
            } else {
                System.out.println("Done.");

                File f = new File(".\\LibDetection\\CONCLUDE\\clusters-" + "graphFile_" + filename + ".txt");
                FileInputStream fis = new FileInputStream(f);
                //Construct BufferedReader from InputStreamReader
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));

                communities = new LinkedList<>();
                line = null;
                int nbcomm = 0;
                while ((line = br.readLine()) != null) {
                    //check if last line
                    if (!line.contains("Q = ")) {
                        System.out.println("comm:" + nbcomm + " == " + line);
                        String[] nodes = line.split("\\W+");
                        System.out.println("The number of nodes is: " + nodes.length);
                        communities.add(new SingleGraph(""));
                        for (String nodeId : nodes) {
                            if ((communities.get(nbcomm).getNode(nodeId)) == null) {
                                communities.get(nbcomm).addNode(nodeId);
                            }
                        }
                        nbcomm++;
                    }
                    //communities.add(new Community(new LinkedList(Arrays.asList(nodes)),null));
                }
                br.close();
                f.delete();

                //Delete weights file
                f = new File(".\\LibDetection\\CONCLUDE\\weights-" + "graphFile_" + filename + ".txt");
                f.delete();

                //add the edges for each community
                f = new File(filePath);
                fis = new FileInputStream(f);
                //Construct BufferedReader from InputStreamReader
                br = new BufferedReader(new InputStreamReader(fis));

                line = null;
                //Read the file line by line and affect the edge to the community
                while ((line = br.readLine()) != null) {
                    System.out.println("line: == " + line);
                    String[] nodes = line.replaceAll("(^\\s+|\\s+$)", "").split("\\s+");
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
            }

        } catch (Exception err) {
            err.printStackTrace();
        }

        return communities;
    }

}
