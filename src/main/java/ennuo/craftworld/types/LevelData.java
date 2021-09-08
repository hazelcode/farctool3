package ennuo.craftworld.types;

import ennuo.craftworld.swing.FileData;
import ennuo.craftworld.swing.FileModel;
import ennuo.craftworld.swing.FileNode;
import ennuo.craftworld.swing.Nodes;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
 
public class LevelData extends FileData {
    
    public boolean isParsed = true;
    int thingsCount = 0;
    
    public LevelData(File file) {
        model = new FileModel(new FileNode("Level", null, null)); 
        root = (FileNode)model.getRoot();
        type = "Level";
        name = file.getName();
        path = file.getAbsolutePath();
        readJSON(file);
    }
    
    public void setThingsCount(int count) {
        this.thingsCount = count;
    }
    
    public void readJSON(File file) {
        JSONParser jsonParser = new JSONParser();         
        try (FileReader reader = new FileReader(file))
        {
            JSONArray obj = (JSONArray)((JSONObject)((JSONObject)((JSONObject)((JSONObject)jsonParser.parse(reader)).get("world")).get("parts")).get("World")).get("things");
            Nodes.addNode(root, new FileEntry("Workspace/"));
            for (int i = 0; i < obj.size(); i++) {
                if (obj.get(i) instanceof org.json.simple.JSONObject) {
                    JSONObject parent = (JSONObject) obj.get(i);
                    Object myParent = null;
                    if (parent.get("parent")!=null) {
                        myParent = parent.get("parent");
                    }
                    JSONObject parts = (JSONObject) parent.get("parts");
                    String parentUID = "";
                    if (myParent!=null) {
                        parentUID = myParent + "/";
                    }
                    Nodes.addNode(root, new FileEntry(parentUID + parent.get("UID")));
                    System.out.println(parentUID + parent.get("UID"));
                    for(Iterator iterator = parts.keySet().iterator(); iterator.hasNext();) {
                        String key = (String) iterator.next();
                        Nodes.addNode(root, new FileEntry(parentUID + parent.get("UID") + "/" + key));
                        System.out.println(parentUID + parent.get("UID") + "/" + key);
                    }
                } else {
                    if (obj.get(i)!=null)
                        Nodes.addNode(root, new FileEntry("" + obj.get(i)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
