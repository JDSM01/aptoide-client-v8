package cm.aptoide.pt.spotandshareandroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by filipe on 31-03-2017.
 */

public class GroupValidator {

  private HashMap<String, Group> ghostsClearHashmap;

  public GroupValidator() {
  }

  public boolean filterSSID(String ssid) {
    if (ssid.contains("APTXV")) {
      return true;
    }
    return false;
  }

  public ArrayList<Group> flagGhosts(ArrayList<Group> groupsList) {
    ghostsClearHashmap = new HashMap<>();
    List<Group> ruleOneGroups = new ArrayList<>();
    for (int i = 0; i < groupsList.size(); i++) {
      String groupDeviceID = groupsList.get(i).getDeviceID();
      String hotspotCounter = groupsList.get(i).getHotspotControlCounter();

      if (!groupDeviceID.equals("")) {//to avoid rule 1 - default groupDeviceID = ""
        if (!ghostsClearHashmap.containsKey(groupDeviceID)) {
          ghostsClearHashmap.put(groupDeviceID, groupsList.get(i));
          groupsList.get(i).setGhostFlag(false);
        } else if ((int) hotspotCounter.charAt(0) > (int) ghostsClearHashmap.get(groupDeviceID)
            .getHotspotControlCounter()
            .charAt(0)) {
          ghostsClearHashmap.put(groupDeviceID, groupsList.get(i));
          groupsList.get(i).setGhostFlag(true);
        }
      } else {
        ruleOneGroups.add(groupsList.get(i));
      }
    }

    ArrayList<Group> list = new ArrayList<Group>(ghostsClearHashmap.values());
    list.addAll(ruleOneGroups);
    return list;
  }

  public ArrayList<Group> removeGhosts(ArrayList<Group> clientsList) {
    ArrayList<Group> clearedList = new ArrayList<>();
    for (int i = 0; i < clientsList.size(); i++) {
      if (!clientsList.get(i).isGhost()) {
        clearedList.add(clientsList.get(i));
      }
    }
    return clearedList;
  }
}