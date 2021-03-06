/*
 *    Copyright 2012 Christian Stussak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.mfo.formulamorph;

import java.util.Random;
import java.util.*;
import javax.swing.SwingUtilities;

import de.mfo.formulamorph.GUI.SurfaceGUIElements;



public class Robot implements Runnable
{
	Random rand = new Random();
	static int switchFormula = 1;
	static int changeParameter = 7;
	static int rotateSurfaces = 1;
	Set< Parameter > freeParameters = Collections.synchronizedSet( EnumSet.complementOf( Surface.M.getParameters() ) );
	boolean switchF = true;
	long enableTime = Long.MAX_VALUE;
	
	public void run()
	{
		holdBack();
		int waitTimeS = 2;
		while( true )
		{
			// wait random amount of time before starting new
			//System.out.println( "waiting " + waitTimeS + "s");
			try{ Thread.sleep( waitTimeS * 1000 ); } catch( Exception e ) {}
			if( System.currentTimeMillis() < enableTime )
			{
				waitTimeS = 1;
				continue;
			}
			
			// start next action
	
			int task = rand.nextInt( switchFormula + changeParameter + rotateSurfaces );
			if( task < switchFormula )
			{
				System.out.println( "Robot: Switching formula");
				// switch either left or right surface
				SwingUtilities.invokeLater( new Runnable(){ public void run() { Main.gui().selectRandomSurface( switchF ? Surface.F : Surface.G, rand ); } } );
				switchF = !switchF;
				waitTimeS = 2;
			}
			else if( task < switchFormula + changeParameter )
			{
				// start animation of parameter
				List< Parameter > freeParmeterList = Arrays.asList( freeParameters.toArray( new Parameter[ 0 ] ) );
				Collections.shuffle( freeParmeterList, rand );
				Parameter p_to_use = null;
				for( Parameter p : freeParmeterList )
				{
					if( p.isActive() )
					{
						p_to_use = p;
						break;
					}
				}
				if( p_to_use != null )
				{
					System.out.println( "Robot: Changing parameter");

					double current = ( p_to_use.getValue() - p_to_use.getMin() ) / ( p_to_use.getMax() - p_to_use.getMin() );
					double target = Math.max( rand.nextDouble() * 1.00001, 1.0 );
							
					int animTime = 5 + rand.nextInt( 5 );
					if( current > target )
						new Thread( new ParameterAnimation( p_to_use, target, current, animTime ) ).start();
					else
						new Thread( new ParameterAnimation( p_to_use, current, target, animTime ) ).start();
					waitTimeS = 3;
				}
                else
                    waitTimeS = 1;
			}
			else
			{
				System.out.println( "Robot: Rotation");
				// rotate
				Main.gui().resumeAnimation();
				new Thread( new StopAnimation( 5 + rand.nextInt( 5 ) ) ).start();
				waitTimeS = 3;
			}
		}
	}
	
	public void holdBack()
	{	
		enableTime = System.currentTimeMillis() + Constants.screensaver_after_seconds * 1000;
		Main.gui().pauseAnimation();
	}
	
	class ParameterAnimation implements Runnable
	{
		Parameter p;
		double min;
		double max;
		int s;
        boolean done;
		
		public ParameterAnimation( Parameter p, double min, double max, int secondsToAnimate ) // min and max in [0,1]
		{
			Robot.this.freeParameters.remove( p );
			this.p = p;
			this.min = min;
			this.max = max;
			this.s = secondsToAnimate;
            done = false;
            final ParameterAnimation pa = this;
            new Thread( new Runnable()
            {
                // let LED blink
                public void run()
                {
                    boolean on = false;
                    while( !pa.done )
                    {
                        try{ Thread.sleep( 250 ); } catch( Exception e ) {}
                        if( !pa.p.isActive() )
                            break;
                        on = !on;
                        Main.phidgetInterface().setLEDEnabled( pa.p, on );
                    }
                    Main.phidgetInterface().setLEDEnabled( pa.p, pa.p.isActive() );
                }
            } ).start();
		}

		public void run()
		{
			double t = min;
			double fps = 25;
			while( t < max )
			{
				try{ Thread.sleep( ( int ) ( 1000 / fps ) ); } catch( Exception e ) {}
				if( System.currentTimeMillis() < enableTime || !p.isActive() )
					break;
				p.setInterpolatedValue( t );
				t += ( min + max ) / ( s * fps );
			}
			Robot.this.freeParameters.add( p );
            done = true;
		}
	}
	
	class StopAnimation implements Runnable
	{
		int s;
		public StopAnimation( int secondsToWait )
		{
			s = secondsToWait;
		}
		
		public void run()
		{
			try{ Thread.sleep( s * 1000 ); } catch( Exception e ) {}
			Main.gui().pauseAnimation();
		}
	}	
}
