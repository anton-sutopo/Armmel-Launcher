package armmel.contacts.dto;

import java.util.LinkedList;
import java.util.List;

public class Search {
  private Long contactId;
  private String name;
  private List<String> result;

  public Search(Long contactId, String name) {
    this.contactId = contactId;
    this.name = name;
  }

  public Long getContactId() {
    return contactId;
  }

  public void setContactId(Long contactId) {
    this.contactId = contactId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getResult() {
    if (result == null) result = new LinkedList<>();
    return result;
  }

  public void setResult(List<String> result) {
    this.result = result;
  }

  public String getShow() {
    StringBuilder s = new StringBuilder(name);
    if (result != null && !result.isEmpty()) {
      s.append(" (");
      for (int i = 0; i < result.size(); i++) {
        s.append(result.get(i));
        if (i < result.size() - 1) {
          s.append(",");
        }
      }
      s.append(")");
    }
    return s.toString();
  }
}
