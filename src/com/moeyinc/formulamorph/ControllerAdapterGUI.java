package com.moeyinc.formulamorph;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import com.moeyinc.formulamorph.Parameters.*;

import java.util.EnumMap;
import java.util.Hashtable;

public class ControllerAdapterGUI extends JFrame implements Controller, Parameters.ValueChangeListener, Parameters.ActivationStateListener {

	private Controller c;
	
	final static int maxSliderValue = 10000;
	//final static int sliderMajorTicks = 5;
	private EnumMap< Parameters.Parameter, JSlider > p2s = new EnumMap< Parameters.Parameter, JSlider >( Parameters.Parameter.class );
	
	public ControllerAdapterGUI( Controller c )
	{
		super( "Controller GUI" );
		if( c == null )
			this.c = new Controller() { // Dummy adapter to ensure that c is not null
				};
		else
			this.c = c;	
		
		Container content = getContentPane();
		content.setLayout( new FlowLayout() );
		for( Parameters.Parameter param : Parameters.Parameter.values() )
		{
			final Parameter p = param;
			JPanel slider_panel = new JPanel();
			slider_panel.setLayout( new BoxLayout( slider_panel, BoxLayout.Y_AXIS ) );
			final JSlider s = new JSlider( JSlider.VERTICAL, 0, maxSliderValue, maxSliderValue / 2 );
			s.addChangeListener( new ChangeListener() { public void stateChanged( ChangeEvent e ) { p.setInterpolatedValue( s.getValue() / (double) maxSliderValue ); } } );
			slider_panel.add( s );
			slider_panel.add( new JLabel( p.name() ) );
			s.setMajorTickSpacing( maxSliderValue / 5 );
			s.setMinorTickSpacing( maxSliderValue / 50 );
			s.setPaintTicks(true);
			s.setPaintLabels( true );
			p.addActivationStateListener( new ActivationStateListener() { public void stateChanged( Parameter p ) { s.setEnabled( p.isActive() ); } });
			p.addValueChangeListener( new ValueChangeListener() { public void valueChanged( Parameter p ) { ControllerAdapterGUI.this.valueChanged( p ); } });
			p2s.put( p, s );
			content.add( slider_panel );
		}
		pack();
		if( isAlwaysOnTopSupported() )
			setAlwaysOnTop( true );
	}
	
	public void valueChanged( Parameter p )
	{
		Hashtable< Integer, JLabel > labelTable = new Hashtable< Integer, JLabel >();
		labelTable.put( new Integer( 0 ), new JLabel( Double.toString(p.getMin())) );
		labelTable.put( new Integer( maxSliderValue / 2 ), new JLabel( Double.toString((p.getMin()+p.getMax())/2)) );
		labelTable.put( new Integer( maxSliderValue ), new JLabel( Double.toString( p.getMax() ) ) );
		JSlider s = p2s.get( p );
		s.setLabelTable( labelTable );
		s.setValue( (int) ( maxSliderValue * ( p.getValue() - p.getMin() ) / ( p.getMax() - p.getMin() ) ) );
	}

	public void stateChanged( Parameter p )	
	{
		p2s.get( p ).setEnabled( p.isActive() );
	}
}
