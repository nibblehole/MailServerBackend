package Controllers;

import Filters.AbstractFilter;
import Filters.ContactFilter;
import Filters.FilterFactory;
import Services.Contact;
import Services.Mail;
import Services.StorageManager;
import Services.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.util.*;


/**
 * controls requests pertaining to the profile of a single user (name,contacts,folders)
 * */
@RestController
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:8081"}, allowCredentials = "true")
public class UserController {

    @GetMapping(value = "/folders/{folderName}")
    public ArrayList<Mail> listMails(@CookieValue(value = "email") String email,
                                     @PathVariable String folderName,
                                     @RequestParam(name = "sortType", defaultValue = "date") String sortType,
                                     @RequestParam(name = "page", defaultValue = "1") Integer page,
                                     @RequestParam(name = "filterType", required = false) String filterType,
                                     @RequestParam(name = "filterValue", required = false) String filterValue) {


        System.out.println(sortType);
        ArrayList<Mail> mails = StorageManager.getUserMails(email, folderName);
        //return mails;
        //sorts in place
        StorageManager.sortMails(mails, sortType);
        System.out.println(mails.size());
        if(filterType != null && filterValue != null) {
            AbstractFilter filter = FilterFactory.getFilter(filterType);
            if(filter != null){
                mails = filter.filter(mails, filterValue);
            }
        }
        return StorageManager.getPage(mails, page);
    }

    @PutMapping("/updateContact")
    public static Contact updateContact(@CookieValue(value = "email") String email,
                                  @RequestBody Contact contact) throws JsonProcessingException {
        
        User user = StorageManager.retrieveUser(email);
        user.getContacts().put(contact.getId(), contact);
        StorageManager.storeUser(user);
        return user.getContacts().get(contact.getId());
    }


    @DeleteMapping("/deleteContact")
    public static void deleteContact(@CookieValue(value = "email") String email,
                                  @RequestBody String id) throws JsonProcessingException {
        User user = StorageManager.retrieveUser(email);
        user.getContacts().remove(id);
        StorageManager.storeUser(user);
        return ;
    }


    @GetMapping("/getContacts")
    public static ArrayList<Contact> getContacts(@CookieValue(value = "email") String email,
                                                 @RequestParam(name = "searchString", defaultValue = "") String searchString) throws JsonProcessingException {
        User user = StorageManager.retrieveUser(email);
        System.out.println(searchString);
        ArrayList<Contact> contacts = new ArrayList<Contact>(user.getContacts().values());
        
        Collections.sort(contacts, new Comparator<Contact>() {
            public int compare(Contact c1, Contact c2) {
                return c1.getName().compareTo(c2.getName());
            }
        });

        return ContactFilter.filter(contacts, searchString);
    }


    @PutMapping("/copy")
    public static boolean copyMailToFolder(@RequestBody String body, @CookieValue(value = "email") String email) {
        JSONObject json = new JSONObject(body);
        String id = json.getString("id");
        String folder = json.getString("to");

        // Create a copy of the mail and store it
        Mail copy = StorageManager.getMail(id.toString()).clone();
        StorageManager.storeMail(copy);

        return StorageManager.addMailToFolder(copy.getID(), folder, email);
    }

    @PutMapping("/move")
    public static boolean MoveMailToFolder(@RequestBody String body, @CookieValue(value = "email") String email) {
        JSONObject json = new JSONObject(body);
        String id = (String) json.getString("id");
        String folderOrigin = json.getString("from");
        String folderDest = json.getString("to");
        return StorageManager.MoveMailToFolder(id, folderOrigin, folderDest, email);
    }

    @DeleteMapping("/remove/{folder}")
    public static boolean removeMailFromFolder(@RequestBody String id, @PathVariable String folder, @CookieValue(value = "email") String email) {
        return StorageManager.MoveMailToTrash(id, folder, email);
    }


    @GetMapping("/getFolders")
    public static String[] getFolders(@CookieValue(value="email") String email){
        User user = StorageManager.retrieveUser(email);
        return user.getFolders().keySet().stream().toArray(String[]::new);
    }

    @DeleteMapping("/removeFolder")
    public static boolean removeFolder(@RequestBody String body, @CookieValue(value = "email") String email){
        JSONObject json = new JSONObject(body);

        String folder = json.getString("folder");
        User user = StorageManager.retrieveUser(email);

        return StorageManager.removeFolder(user, folder.toLowerCase());
    }

    @DeleteMapping("/removeMultipleMails/{folderName}")
    public static String removeMultipleMails(@RequestBody(required = false) ArrayList<String> emailIDArray,
                                            @PathVariable String folderName,
                                            @CookieValue(value = "email") String email)
    {

        if(emailIDArray == null || emailIDArray.size() == 0)
            return "no emails to be deleted";

        System.out.println("Emails to be Deleted");
        System.out.println(emailIDArray.toString());
        System.out.println("====================");

        System.out.println("Folder to be Deleted from");
        System.out.println("====================");
        
        for(String id: emailIDArray) {
            StorageManager.MoveMailToTrash(id, folderName, email);
        }
        return "emails deleted successfully";
    }

    @PutMapping("/copyMultipleMails")
    public static String copyMultipleMails(@RequestBody String body,
                                            @CookieValue(value = "email") String email)
    {
        JSONObject json = new JSONObject(body);
        List<Object> emailIDArray = json.getJSONArray("array").toList();
        String from = json.getString("from");
        String to = json.getString("to");
        if(emailIDArray == null || emailIDArray.size() == 0 || from == null || to == null)
            return "an error occurred";
        System.out.println(from);
        System.out.println(to);
        for(Object id: emailIDArray) {

            // Create a copy of each mail and store it
            Mail copy = StorageManager.getMail(id.toString()).clone();
            StorageManager.storeMail(copy);
            StorageManager.CopyMailToFolder(copy.getID(), to, email);
        }
        return "emails copied successfully";
    }


    @PutMapping("/moveMultipleMails")
    public static String moveMultipleMails(@RequestBody String body,
                                            @CookieValue(value = "email") String email)
    {
        JSONObject json = new JSONObject(body);
        List<Object> emailIDArray = json.getJSONArray("array").toList();
        String from = json.getString("from");
        String to = json.getString("to");
        if(emailIDArray == null || emailIDArray.size() == 0 || from == null || to == null)
            return "an error occurred";
        System.out.println(from);
        System.out.println(to);
        for(Object id: emailIDArray) {
            StorageManager.MoveMailToFolder(id.toString(), from, to, email);
        }
        return "emails moved successfully";
    }




    @PostMapping("/addFolder")
    public static boolean addFolder(@RequestBody String body, @CookieValue(value = "email") String email){
        JSONObject json = new JSONObject(body);

        String folder = json.getString("folder");
        User user = StorageManager.retrieveUser(email);

        return StorageManager.addFolder(user, folder.toLowerCase());
    }

    @PostMapping("/renameFolder")
    public static boolean renameFolder(@RequestBody String body, @CookieValue(value = "email") String email){
        JSONObject json = new JSONObject(body);

        String oldFolder = json.getString("oldFolder");
        String newFolder = json.getString("newFolder");
        User user = StorageManager.retrieveUser(email);

        return StorageManager.renameFolder(user, oldFolder.toLowerCase(), newFolder.toLowerCase());
    }

    @PostMapping("/changePassword")
    public static boolean changePassword(@RequestBody String body, @CookieValue(value = "email") String email){
        JSONObject json = new JSONObject(body);

        String oldPassword = json.getString("oldPassword");
        String newPassword = json.getString("newPassword");
        User user = StorageManager.retrieveUser(email);

        return StorageManager.changePassword(user, oldPassword, newPassword);
    }
}
