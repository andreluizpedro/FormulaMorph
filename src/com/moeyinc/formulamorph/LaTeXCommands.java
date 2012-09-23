package com.moeyinc.formulamorph;

import org.scilab.forge.jlatexmath.Atom;
import org.scilab.forge.jlatexmath.EmptyAtom;
import org.scilab.forge.jlatexmath.ScaleAtom;
import org.scilab.forge.jlatexmath.ResizeAtom;
import org.scilab.forge.jlatexmath.RotateAtom;
import org.scilab.forge.jlatexmath.TeXParser;
import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.ParseOption;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXEnvironment;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.SpaceAtom;
import org.scilab.forge.jlatexmath.Box;
import org.scilab.forge.jlatexmath.FBoxAtom;
import org.scilab.forge.jlatexmath.FramedBox;
import org.scilab.forge.jlatexmath.dynamic.DynamicAtom;
import org.scilab.forge.jlatexmath.dynamic.ExternalConverter;
import org.scilab.forge.jlatexmath.dynamic.ExternalConverterFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.geom.RoundRectangle2D;
import java.awt.RenderingHints;

import java.awt.image.ImageObserver;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.MediaTracker;
import java.awt.Label;
import java.awt.Toolkit;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class LaTeXCommands {

	static {
		String xml = "" +
			"<PredefinedCommands enabled=\"true\">\n" +
			"	<Command name=\"FMDynamic\" enabled=\"true\">\n" +
			"		<CreateCommand name=\"f\">\n" +
			"			<Argument type=\"String\" value=\"com.moeyinc.formulamorph.LaTeXCommands\" />\n" +
			"			<Argument type=\"String\" value=\"formulaMorphDynamic_macro\" />\n" +
			"			<!-- the command includeJavaImage has 1 arguments (the key to replace)-->\n" +
			"			<Argument type=\"float\" value=\"1\" />\n" +
			"			<!-- and an optional argument placed before the main argument -->\n" +
			"			<Argument type=\"float\" value=\"1\" />\n" +
			"		</CreateCommand>\n" +
			"		<Return name=\"f\" />\n" +
			"	</Command>\n" +
			"	<Command name=\"includejavaimage\" enabled=\"true\">\n" +
			"		<CreateCommand name=\"f\">\n" +
			"			<Argument type=\"String\" value=\"com.moeyinc.formulamorph.LaTeXCommands\" />\n" +
			"			<Argument type=\"String\" value=\"includeJavaImage_macro\" />\n" +
			"			<!-- the command includeJavaImage has 1 arguments (name of the image)-->\n" +
			"			<Argument type=\"float\" value=\"1\" />\n" +
			"			<!-- and an optional argument placed before the main argument (e.g. \\includejavaimage[abc]{Hello}) -->\n" +
			"			<Argument type=\"float\" value=\"1\" />\n" +
			"		</CreateCommand>\n" +
			"		<Return name=\"f\" />\n" +
			"	</Command>\n" +
/*			"	<Command name=\"ovalfcolorbox\" enabled=\"true\">\n" +
			"		<CreateCommand name=\"f\">\n" +
			"			<Argument type=\"String\" value=\"Foo.FooPackage\" />\n" +
			"			<Argument type=\"String\" value=\"ovalFColorBox_macro\" />\n" +
			"			<!-- the command ovalcolorfbox has 2 arguments (color and text)-->\n" +
			"			<Argument type=\"float\" value=\"2\" />\n" +
			"		</CreateCommand>\n" +
			"		<Return name=\"f\" />\n" +
			"	</Command>\n" +*/
			"</PredefinedCommands>\n";

        TeXFormula.addPredefinedCommands( new ByteArrayInputStream( xml.getBytes()) );
	}

	private static final Map< String, String > dynamicLaTeXStrings;
	static final ExternalConverter extConv;
	static
	{		
		dynamicLaTeXStrings = new HashMap< String, String >();
		extConv = new ExternalConverter() {
				public String getLaTeXString( String externalCode ) {
					String s = LaTeXCommands.dynamicLaTeXStrings.get( externalCode );
					System.out.println( externalCode + ": " + s );
					return s == null ? "\\text{no value for \\backslash jlmDynamic\\{"+externalCode+"\\}}" : s;
				}
			};
		DynamicAtom.setExternalConverterFactory( new ExternalConverterFactory() {
			public ExternalConverter getExternalConverter() { return extConv; }
		} );
	}
	public static Map< String, String > getDynamicLaTeXMap() { return dynamicLaTeXStrings; }
	
    private static TeXFormula formula = new TeXFormula();
    public Atom formulaMorphDynamic_macro(TeXParser tp, String[] args) throws ParseException {
    	
    	String tex;
    	if( args.length > 2 && args[ 2 ].equals( "i" ) )
    		tex = extConv.getLaTeXString( args[ 1 ] );
    	else
    		tex = "\\jlmDynamic{" + args[ 1 ] + "}";
		formula.setLaTeX( tex );
		return formula.root != null ? formula.root : new EmptyAtom();
	}
	
	static Map<String,Image> images = new HashMap< String, Image >();

	public static Map<String,Image> getImageMap() { return images; }
    
    public Atom includeJavaImage_macro(TeXParser tp, String[] args) throws ParseException {
        return new GraphicsAtom(getImageMap().get(args[1]), args[2]);
    }
    
	/**
	 * An atom representing an atom containing a graphic.
	 */
	public class GraphicsAtom extends Atom {
		
		private Image image = null;
		private BufferedImage bimage;
		private Label c;
		private int w, h;

		private Atom base;
		private boolean first = true;
		private int interp = -1;

		public GraphicsAtom( Image bi, String option) {
		image=bi;
		draw();
		buildAtom(option);
		}

		protected void buildAtom(String option) {
		base = this;
			Map<String, String> options = ParseOption.parseMap(option);
		if (options.containsKey("width") || options.containsKey("height")) {
			base = new ResizeAtom(base, options.get("width"), options.get("height"), options.containsKey("keepaspectratio"));
		}
		if (options.containsKey("scale")) {
			double scl = Double.parseDouble(options.get("scale"));
			base = new ScaleAtom(base, scl, scl); 
		}
		if (options.containsKey("angle") || options.containsKey("origin")) {
			base = new RotateAtom(base, options.get("angle"), options.get("origin"));
		}
		if (options.containsKey("interpolation")) {
			String meth = options.get("interpolation");
			if (meth.equalsIgnoreCase("bilinear")) {
			interp = GraphicsBox.BILINEAR;
			} else if (meth.equalsIgnoreCase("bicubic")) {
			interp = GraphicsBox.BICUBIC;
			} else if (meth.equalsIgnoreCase("nearest_neighbor")) {
			interp = GraphicsBox.NEAREST_NEIGHBOR;
			}
		}
		}
	
		public void draw() {
		if (image != null) {
			w = image.getWidth(c);
			h = image.getHeight(c);
			bimage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = bimage.createGraphics();
			g2d.drawImage(image, 0, 0, null);
			g2d.dispose();
		}
		}

		public Box createBox(TeXEnvironment env) {
		if (image != null) {
			if (first) {
			first = false;
			return base.createBox(env);
			} else {
			env.isColored = true;
			float width = w * SpaceAtom.getFactor(TeXConstants.UNIT_PIXEL, env);
			float height = h * SpaceAtom.getFactor(TeXConstants.UNIT_PIXEL, env);
			return new GraphicsBox(bimage, width, height, env.getSize(), interp);
			}
		}

		return new TeXFormula("\\text{ No such image file ! }").root.createBox(env);
		}
	}
	
	public class GraphicsBox extends Box {

	    public final static int BILINEAR = 0;
	    public final static int NEAREST_NEIGHBOR = 1;
	    public final static int BICUBIC = 2;

	    private BufferedImage image;
	    private float scl;
	    private Object interp;
	    private int interpolation;

	    public GraphicsBox(BufferedImage image, float width, float height, float size, int interpolation) {
		this.image = image;
		this.width = width;
		this.height = height;
		this.scl = 1 / size;
	    this.interpolation = interpolation;
		depth = 0;
		shift = 0;
		switch (interpolation) {
		case BILINEAR :
		    interp = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
		    break;
		case NEAREST_NEIGHBOR :
		    interp = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
		    break;
		case BICUBIC :
		    interp = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
		    break;
		default :
		    interp = null;
		}
	    }
	   
	    public void draw(Graphics2D g2, float x, float y) {
		AffineTransform oldAt = g2.getTransform();
		Object oldKey = null;
		if (interp != null) {
		    oldKey = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interp);
		}
		g2.translate(x, y - height);
		double scl_abs = Math.abs( scl );
		double scl_sgn = Math.signum( scl );
		g2.scale( scl_sgn, scl_sgn );
		if( scl >= 1 )
		{
			g2.scale(scl_abs, scl_abs);
			g2.drawImage(image, 0, 0, null);
		}
		else
		{
			double globalsclx = Math.abs( g2.getTransform().getScaleX() );
			double globalscly = Math.abs( g2.getTransform().getScaleY() );
			g2.scale( 1.0 / globalsclx, 1.0 / globalscly );
			int ws = (int) (image.getWidth() * globalsclx * scl_abs );
			int hs = (int) (image.getHeight() * globalscly * scl_abs );
			Image scaledImage;
			switch( interpolation )
			{
			case NEAREST_NEIGHBOR:
				scaledImage = image.getScaledInstance( ws > 0 ? ws : 1, hs > 0 ? hs : 1, Image.SCALE_FAST );
				break;
			case BILINEAR:
			case BICUBIC:
			default:
				scaledImage = image.getScaledInstance( ws > 0 ? ws : 1, hs > 0 ? hs : 1, Image.SCALE_SMOOTH );
				break;
			}
			g2.drawImage( scaledImage, 0, 0, null );
		}
			 
		g2.setTransform(oldAt);
		if (oldKey != null) {
		    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldKey);
		}
	    }
	    
	    public int getLastFontId() {
		return 0;
	    }
	}	
    
/*
    public Atom ovalFColorBox_macro(TeXParser tp, String[] args) throws ParseException {
        return new TeXFormula( args[1] ).root;
    }

	public class OvalColorAtom extends FBoxAtom {

		public OvalColorAtom(Atom base) {
		super(base);
		}
		
		public Box createBox(TeXEnvironment env) {
		return new OvalFColorBox((FramedBox) super.createBox(env));
		}
	}

	public class OvalFColorBox extends FramedBox {
		
		private float shadowRule;

		public OvalFColorBox(FramedBox fbox) {
		super(fbox.box, fbox.thickness, fbox.space);
		}

		public void draw(Graphics2D g2, float x, float y) {
		box.draw(g2, x + space + thickness, y);
		Stroke st = g2.getStroke();
		g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
		float th = thickness / 2;
		float r = 0.5f * Math.min(width - thickness, height + depth - thickness);
		g2.draw(new RoundRectangle2D.Float(x + th, y - height + th, width - thickness, height + depth - thickness, r, r));
		//drawDebug(g2, x, y);
		g2.setStroke(st);
		}

		public int getLastFontId() {
		return box.getLastFontId();
		}
	}
*/
}
