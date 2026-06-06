package sp;

public class SPConfig {
    public static SPConfig instance = new SPConfig();

    public int particleLimit = 5000;
    public boolean smartCameraCulling = true;

    public static SPConfig get() {
        return instance;
    }
}
