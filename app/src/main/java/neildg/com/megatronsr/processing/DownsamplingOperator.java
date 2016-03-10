package neildg.com.megatronsr.processing;

import android.graphics.Bitmap;

import neildg.com.megatronsr.constants.FilenameConstants;
import neildg.com.megatronsr.io.ImageWriter;
import neildg.com.megatronsr.preprocessing.BitmapURIRepository;

/**
 * Created by NeilDG on 3/5/2016.
 */
public class DownsamplingOperator {
    private static String TAG = "DownsamplingOperator";

    private int downsampleFactor = 2;

    public DownsamplingOperator(int downsampleFactor) {
        this.downsampleFactor = downsampleFactor;
    }

    public void perform() {
        ImageWriter imageWriter = ImageWriter.getInstance();
        imageWriter.createNewAlbum();
        BitmapURIRepository bitmapURIRepository = BitmapURIRepository.getInstance();
        for(int i = 0; i < BitmapURIRepository.getInstance().getNumImages(); i++) {
            Bitmap downsampledBitmap = bitmapURIRepository.getDownsampledBitmap(i, this.downsampleFactor);
            imageWriter.saveBitmapImage(downsampledBitmap, FilenameConstants.DOWNSAMPLE_PREFIX_STRING+i);
            downsampledBitmap.recycle();
        }
    }
}