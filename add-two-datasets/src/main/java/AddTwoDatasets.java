/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.widget.FileWidget;
import net.imagej.ops.Op;

import java.io.File;
import java.io.IOException;

/** Adds two datasets using the ImgLib2 framework. */
public class AddTwoDatasets {

	public static void main(final String... args) throws Exception {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		ij.ui().showUI();

		// load two datasets
		final Dataset dataset1 = load(ij);

		if (dataset1 == null) {
		    ij.ui().showDialog("Load dataset failed.");
		    return;
        }

		final Dataset dataset2 = load(ij);

		if (dataset2 == null) {
		    ij.ui().showDialog("Load dataset failed.");
		    return;
        }

		if (dataset1.numDimensions() != dataset2.numDimensions()) {
			ij.ui().showDialog("Input datasets must have the same number of dimensions.");
			return;
		}

		// add them together
		final Dataset result1 = addRandomAccess(ij, dataset1, dataset2);
		//final Dataset result2 = addOpsSerial(ij, dataset1, dataset2, new FloatType());
		//final Dataset result3 = addOpsParallel(ij, dataset1, dataset2, new FloatType());

		// display the results
		ij.display().createDisplay(dataset1.getName(), dataset1);
		ij.display().createDisplay(dataset2.getName(), dataset2);
		ij.display().createDisplay("Result: random access", result1);
		//ij.display().createDisplay("Result: serial OPS", result2);
		//ij.display().createDisplay("Result: parallel OPS", result3);
	}

	/**
	 * Adds two datasets using a loop with an ImgLib cursor. This is a very
	 * powerful approach but requires a verbose loop.
	 */
	@SuppressWarnings({ "rawtypes" })
	private static Dataset addRandomAccess(final ImageJ ij, final Dataset d1, final Dataset d2)
	{
		final Dataset result = create(ij, d1, d2, new FloatType());

		// sum data into result dataset
		final RandomAccess<? extends RealType> ra1 = d1.getImgPlus().randomAccess();
		final RandomAccess<? extends RealType> ra2 = d2.getImgPlus().randomAccess();
		final Cursor<? extends RealType> cursor = result.getImgPlus().localizingCursor();

		final long[] pos1 = new long[d1.numDimensions()];
		final long[] pos2 = new long[d2.numDimensions()];

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos1);
			cursor.localize(pos2);
			ra1.setPosition(pos1);
			ra2.setPosition(pos2);
			final double sum = ra1.get().getRealDouble() + ra2.get().getRealDouble();
			cursor.get().setReal(sum);
		}

		return result;
	}

	/**
	 * Adds two datasets using the ImageJ OPS framework.
     * This is a very succinct approach that does not require a loop.
     * This version is designed for small processing jobs and is not automatically parallelized.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T extends RealType<T> & NativeType<T>> Dataset addOpsSerial(
		final ImageJ ij, final Dataset d1, final Dataset d2, final T outType)
	{
		final Dataset output = create(ij, d1, d2, outType);
		final IterableInterval img1 = d1.getImgPlus();
		final IterableInterval img2 = d2.getImgPlus();
		final IterableInterval outputImg = output.getImgPlus();

        ij.op().math().add(outputImg, img1, img2);

		return output;
	}

	/**
	 * Adds two datasets using the ImgLib OPS framework.
     * This is a very succinct approach that does not require a loop.
     * This version is automatically parallelized!
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T extends RealType<T> & NativeType<T>> Dataset addOpsParallel(
	        final ImageJ ij, final Dataset d1, final Dataset d2, final T outType)
	{
		final Dataset output = create(ij, d1, d2, outType);
		final IterableInterval img1 = d1.getImgPlus();
		final IterableInterval img2 = d2.getImgPlus();
		final IterableInterval outputImg = output.getImgPlus();

        final Op addOp = ij.op().op("math.add", IterableInterval.class, IterableInterval.class);
        ij.op().run(addOp, output, img1, img2);

		return output;
	}

	/**
	 * Loads a dataset selected by the user from a dialog box.
     */
	private static Dataset load(final ImageJ ij) throws IOException {
		// ask the user for a file to open
		final File file = ij.ui().chooseFile(null, FileWidget.OPEN_STYLE);

		if (file == null) {
		    return null;
        }

		// load the dataset
        if (ij.scifio().datasetIO().canOpen(file.getAbsolutePath())) {
		    return ij.scifio().datasetIO().open(file.getAbsolutePath());
        } else {
            return null;
        }
	}

	/**
	 * Creates a dataset with bounds constrained by the minimum of the two input datasets.
	 */
	private static <T extends RealType<T> & NativeType<T>> Dataset create(
		final ImageJ ij, final Dataset d1, final Dataset d2, final T type)
    {
		final int dimCount = Math.min(d1.numDimensions(), d2.numDimensions());
		final long[] dims = new long[dimCount];
		final AxisType[] axes = new AxisType[dimCount];

		for (int i = 0; i < dimCount; i++) {
			dims[i] = Math.min(d1.dimension(i), d2.dimension(i));
			axes[i] = d1.numDimensions() > i ? d1.axis(i).type() : d2.axis(i).type();
		}

		return ij.dataset().create(type, dims, "result", axes);
	}

}
