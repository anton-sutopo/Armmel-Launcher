package armmel.contacts.dto;

import android.content.Context;
import android.util.Log;
import armmel.contacts.database.DBHelper;
import armmel.contacts.database.dao.ContactDao;
import armmel.contacts.database.entity.Contact;
import armmel.contacts.database.repository.ContactRepository;
import armmel.contacts.orm.DaoFactory;
import armmel.contacts.utils.Utils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SearchService {
  private Context context;
  private ContactRepository contactDao;
  private final DBHelper dbHelper;

  public SearchService(Context context) {
    this.context = context;
    this.contactDao = new ContactRepository(context);
    this.dbHelper = new DBHelper(context);
  }

  public List<Search> findSearchByEmailTerm(String term) {
    Map<Long, Search> searchMap = new LinkedHashMap<>();

    ContactDao contactDao = DaoFactory.create(ContactDao.class, dbHelper.getReadableDatabase());
    List<SearchDto> lsd = contactDao.getContactWithEmail("%" + term + "%");
    for (SearchDto sd : lsd) {
      long id = sd.getContactId();
      String name = sd.getName();
      String email = sd.getResult();

      Search s = searchMap.get(id);
      if (s == null) {
        s = new Search(id, name);
        searchMap.put(id, s);
      }

      s.getResult().add(email);
    }

    return new ArrayList<>(searchMap.values());
  }

  public List<Search> findSearchByPhoneTerm(String term) {
    Map<Long, Search> searchMap = new LinkedHashMap<>();
    ContactDao contactDao = DaoFactory.create(ContactDao.class, dbHelper.getReadableDatabase());
    List<SearchDto> lsd = contactDao.getContactWithPhones("%" + term + "%");
    for (SearchDto sd : lsd) {
      long id = sd.getContactId();
      String name = sd.getName();
      String phone = sd.getResult();

      Search s = searchMap.get(id);
      if (s == null) {
        s = new Search(id, name);
        searchMap.put(id, s);
      }

      s.getResult().add(phone);
    }

    return new ArrayList<>(searchMap.values());
  }

  public List<Search> getAllFiltered(String term) {
    List<Search> result = new ArrayList<>();
    if (Utils.isPartialPhoneNumber(term)) {
      term = Utils.removeForPhone(term);
      result = findSearchByPhoneTerm(term);
    } else if (term.startsWith("@")) {
      String s = term.length() > 1 ? term.substring(1) : "";
      result = findSearchByEmailTerm(s);
    } else {
      List<Contact> rs = contactDao.getAllFiltered(term);
      for (Contact r : rs) {
        Log.d(SearchService.class.toString(), "id : " + r.getId() + ", name :" + r.getName());
        result.add(new Search(r.getId(), r.getName()));
      }
    }
    result.sort(
        (a, b) -> {
          return a.getName().compareToIgnoreCase(b.getName());
        });
    return result;
  }
}
