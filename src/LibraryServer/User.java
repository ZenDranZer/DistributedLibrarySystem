package LibraryServer;

public class User {

    private String userID;
    private String library;
    private String index;
    private boolean outsourced;

    public User(String userID){
        this.userID = userID;
        this.library = userID.substring(0,3);
        this.index = userID.substring(4);
        outsourced = false;
    }

    public boolean isOutsourced() {
        return outsourced;
    }

    public void setOutsourced(boolean outsourced) {
        this.outsourced = outsourced;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getLibrary() {
        return library;
    }

    public void setLibrary(String library) {
        this.library = library;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
}
