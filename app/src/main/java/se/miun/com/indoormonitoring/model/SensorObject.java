package se.miun.com.indoormonitoring.model;

/**
 * Created by Faustino on 11-8-2016.
 */
public class SensorObject {

    public Double version;
    public Sensors sensors;

    public int getSize() {
        //Hardcoded size
        return 5;
    }
}
