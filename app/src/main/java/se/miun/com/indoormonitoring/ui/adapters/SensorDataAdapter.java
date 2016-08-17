package se.miun.com.indoormonitoring.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import se.miun.com.indoormonitoring.R;
import se.miun.com.indoormonitoring.model.SensorObject;

/**
 * Created by Faustino on 10-8-2016.
 */
public class SensorDataAdapter extends RecyclerView.Adapter<SensorDataAdapter.SensorDataViewHolder> {

    private SensorObject mSensorObject;
    private Context mContext;

    public SensorDataAdapter(Context context, SensorObject object) {
        mSensorObject = object;
        mContext = context;

    }

    @Override
    public SensorDataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_sensor_row, parent, false);
        return new SensorDataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SensorDataViewHolder holder, int position) {
//        holder.sensorImage
        holder.temperatureValue.setText(String.valueOf(mSensorObject.sensors.temperature));
        holder.temperatureValue.setText(String.valueOf(mSensorObject.sensors.humidity));
        holder.temperatureValue.setText(String.valueOf(mSensorObject.sensors.pressure));
        holder.temperatureValue.setText(String.valueOf(mSensorObject.sensors.concentrationGases));
        holder.temperatureValue.setText(String.valueOf(mSensorObject.sensors.combustibleGases));
        holder.temperatureValue.setText(String.valueOf(mSensorObject.sensors.airQuality));

        //todo set images

    }

    @Override
    public int getItemCount() {
        return mSensorObject.getSize();
//        return mSensorList.size();
    }

    public class SensorDataViewHolder extends RecyclerView.ViewHolder {

        ImageView temperatureImage;
        TextView temperatureValue;

        ImageView humidityImage;
        TextView humidityValue;

        ImageView pressureImage;
        TextView pressureValue;

        ImageView concentrationGasesImage;
        TextView concentrationGasesValue;

        ImageView combustibleGasesImage;
        TextView combustibleGasesValue;

        ImageView airQualityImage;
        TextView airQualityValue;

        public SensorDataViewHolder(View itemView) {
            super(itemView);

            temperatureImage = (ImageView) itemView.findViewById(R.id.temperature_image);
            temperatureValue = (TextView) itemView.findViewById(R.id.temperature_value);

            humidityImage = (ImageView) itemView.findViewById(R.id.humidity_image);
            humidityValue = (TextView) itemView.findViewById(R.id.humidity_value);

            pressureImage = (ImageView) itemView.findViewById(R.id.pressure_image);
            pressureValue = (TextView) itemView.findViewById(R.id.pressure_value);

            concentrationGasesImage = (ImageView) itemView.findViewById(R.id.concentration_gases_image);
            concentrationGasesValue = (TextView) itemView.findViewById(R.id.concentration_gases_value);

            combustibleGasesImage = (ImageView) itemView.findViewById(R.id.combustible_gases_image);
            combustibleGasesValue = (TextView) itemView.findViewById(R.id.combustible_gases_value);

            airQualityImage = (ImageView) itemView.findViewById(R.id.air_quality_image);
            airQualityValue = (TextView) itemView.findViewById(R.id.air_quality_value);

        }
    }
}
