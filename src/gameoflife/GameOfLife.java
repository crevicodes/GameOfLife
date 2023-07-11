/**
 * CMP256 - GUI Design and Programming
 * Dr. Michel Pasquier
 * Spring 2022 Course Project
 * An Adaptation of Conway's Game of Life
 * @author Harish Menon
 * @author Angelo Sebastian Cabarloc
 * @author Koushal Parupudi
 */
package gameoflife;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class GameOfLife
{
    public static void main(String[] args)
    {
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                Model model = new Model();
                View view = new View(model);
                new Controller(model, view);
            }
        });
    }
}

class Cell
{
    private int neighbours;
    private boolean currentState;
    private boolean nextState;

    public Cell()
    {
        neighbours = 0;
        currentState = false;
        nextState = false;
    }

    public int getNeighbours()
    {
        return neighbours;
    }

    public void incrementNeighbours()
    {
        neighbours++;
    }

    public void resetNeighbours()
    {
        neighbours = 0;
    }

    public boolean isLiving()
    {
        return currentState;
    }

    public void setLiving(boolean currentState)
    {
        this.currentState = currentState;
    }

    public boolean willLive()
    {
        return nextState;
    }

    public void setNextState(boolean nextState)
    {
        this.nextState = nextState;
    }
}

class GamePanel extends JPanel
{
    private Graphics2D g2;
    private final Model model;
    private boolean initialize = true;

    public GamePanel(Model model)
    {
        this.model = model;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        setFocusable(true);
        g2 = (Graphics2D) g;
        g2.setColor(model.getBackgroundColor());
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(model.getDeadColor());
        g2.fillRect(model.getCamX(), model.getCamY(), model.getGridSize(), model.getGridSize());
        g2.setColor(model.getGridlineColor());
        drawGrid();
        g2.setColor(model.getLivingColor());
        for(int r = 0; r < model.getGrid().length; r++)
        {
            for(int c = 0; c < model.getGrid()[r].length; c++)
            {
                if(model.getGrid()[r][c].isLiving())
                {
                    g2.fillRect(r * model.getSize() + model.getCamX() + 1, c  * model.getSize() + model.getCamY() + 1, model.getSize() - 1, model.getSize() - 1);
                }
            }
        }
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(1000, 600);
    }

    private void drawGrid()
    {
        Dimension gridDim = new Dimension();
        int i;
        for(i = model.getCamX(); i <= model.getGridSize() + model.getCamX(); i += model.getSize())
        {
            if(model.gridVisible())
                g2.drawLine(i, model.getCamY(), i, model.getGridSize() + model.getCamY());
        }
        gridDim.width = i / model.getSize() - 1;
        for(i = model.getCamY(); i <= model.getGridSize() + model.getCamY(); i += model.getSize())
        {
            if(model.gridVisible())
                g2.drawLine(model.getCamX(), i, model.getGridSize() + model.getCamX(), i);
        }
        gridDim.height = i / model.getSize() - 1;
        if(initialize)
        {
            model.clearGrid(gridDim.width, gridDim.height);
            model.initializeGrid();
            initialize = false;
        }
    }
}


class Model
{
    private final Dictionary<String, char[][]> ALL_PATTERNS = new Hashtable<>();

    private final String[] ALL_SPEEDS = {"Slow", "Normal", "Fast"};
    private final String[] ALL_SIZES = {"Small", "Medium", "Big"};
    private boolean showGrid;
    private boolean defaultGridVisibility;
    private int speed;
    private int size;
    private int generation = 0;
    private Dimension frameSize;
    private String defaultPattern;
    private String defaultSpeed;
    private String defaultSize;
    private Color deadColor;
    private Color livingColor;
    private Color backgroundColor;
    private Color gridlineColor;
    private Color defaultDeadColor;
    private Color defaultLivingColor;
    private Color defaultBackgroundColor;
    private Color defaultGridlineColor;
    private int gridSize;
    private final int cellMultiple = 100;
    private int camX;
    private int camY;
    private Cell[][] grid;

    public Model()
    {
        try
        {
            Scanner data = new Scanner(new File("preferences.txt"));
            defaultPattern = data.next();
            defaultSpeed = data.next();
            defaultSize = data.next();
            defaultBackgroundColor = getColor(data);
            defaultDeadColor = getColor(data);
            defaultLivingColor = getColor(data);
            defaultGridlineColor = getColor(data);
            frameSize = getDimension(data);
            defaultGridVisibility = data.nextBoolean();
            data.close();
        } catch(FileNotFoundException ex)
        {
            System.out.println("preferences.txt not found - using default values");
            defaultPattern = "Clear";
            defaultSpeed = "Normal";
            defaultSize = "Medium";
            defaultDeadColor = Color.DARK_GRAY;
            defaultLivingColor = Color.BLUE;
            defaultBackgroundColor = Color.GRAY;
            defaultGridlineColor = Color.BLACK;
            frameSize = new Dimension(1000, 750);
            defaultGridVisibility = true;
        }
        switch(defaultSize)
        {
            case "Small":
                size = 5;
                break;
            case "Big":
                size = 35;
                break;
            default:
                size = 20;
        }
        switch(defaultSpeed)
        {
            case "Slow":
                speed = 1550;
                break;
            case "Fast":
                speed = 50;
                break;
            default:
                speed = 800;
        }
        backgroundColor = defaultBackgroundColor;
        deadColor = defaultDeadColor;
        livingColor = defaultLivingColor;
        gridlineColor = defaultGridlineColor;
        showGrid = defaultGridVisibility;

        gridSize = size * cellMultiple;
        ALL_PATTERNS.put("Clear", new char[][]{});
        ALL_PATTERNS.put("Blinker", new char[][]{
                {'Y', 'Y', 'Y'}
        });
        ALL_PATTERNS.put("Block", new char[][]{
                {'Y', 'Y'},
                {'Y', 'Y'}
        });
        ALL_PATTERNS.put("Tub", new char[][]{
                {'N', 'Y', 'N'},
                {'Y', 'N', 'Y'},
                {'N', 'Y', 'N'}
        });
        ALL_PATTERNS.put("Boat", new char[][]{
                {'Y', 'Y', 'N'},
                {'Y', 'N', 'Y'},
                {'N', 'Y', 'N'}
        });
        ALL_PATTERNS.put("Glider", new char[][]{
                {'Y', 'Y', 'Y'},
                {'Y', 'N', 'N'},
                {'N', 'Y', 'N'}
        });
        ALL_PATTERNS.put("Ship", new char[][]{
                {'Y', 'Y', 'N'},
                {'Y', 'N', 'Y'},
                {'N', 'Y', 'Y'}
        });
        ALL_PATTERNS.put("Beehive", new char[][]{
                {'N', 'Y', 'Y', 'N'},
                {'Y', 'N', 'N', 'Y'},
                {'N', 'Y', 'Y', 'N'}
        });
        ALL_PATTERNS.put("Barge", new char[][]{
                {'N', 'Y', 'N', 'N'},
                {'Y', 'N', 'Y', 'N'},
                {'N', 'Y', 'N', 'Y'},
                {'N', 'N', 'Y', 'N'}
        });
        ALL_PATTERNS.put("Toad", new char[][]{
                {'Y', 'Y', 'Y', 'N'},
                {'N', 'Y', 'Y', 'Y'},
        });
        ALL_PATTERNS.put("Beacon", new char[][]{
                {'Y', 'Y', 'N', 'N'},
                {'Y', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'Y'},
                {'N', 'N', 'Y', 'Y'}
        });
        ALL_PATTERNS.put("Long Boat", new char[][]{
                {'Y', 'Y', 'N', 'N'},
                {'Y', 'N', 'Y', 'N'},
                {'N', 'Y', 'N', 'Y'},
                {'N', 'N', 'Y', 'N'}
        });
        ALL_PATTERNS.put("Load", new char[][]{
                {'N', 'Y', 'Y', 'N'},
                {'Y', 'N', 'N', 'Y'},
                {'Y', 'N', 'Y', 'N'},
                {'N', 'Y', 'N', 'N'}
        });
        ALL_PATTERNS.put("Pond", new char[][]{
                {'N', 'Y', 'Y', 'N'},
                {'Y', 'N', 'N', 'Y'},
                {'Y', 'N', 'N', 'Y'},
                {'N', 'Y', 'Y', 'N'}
        });
        ALL_PATTERNS.put("Mango", new char[][]{
                {'N', 'Y', 'Y', 'N', 'N'},
                {'Y', 'N', 'N', 'Y', 'N'},
                {'N', 'Y', 'N', 'N', 'Y'},
                {'N', 'N', 'Y', 'Y', 'N'}
        });
        ALL_PATTERNS.put("Long Barge", new char[][]{
                {'N', 'Y', 'N', 'N', 'N'},
                {'Y', 'N', 'Y', 'N', 'N'},
                {'N', 'Y', 'N', 'Y', 'N'},
                {'N', 'N', 'Y', 'N', 'Y'},
                {'N', 'N', 'N', 'Y', 'N'}
        });
        ALL_PATTERNS.put("Half-Fleet", new char[][]{
                {'Y', 'Y', 'N', 'N', 'N', 'N'},
                {'Y', 'N', 'Y', 'N', 'N', 'N'},
                {'N', 'Y', 'Y', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'Y', 'Y', 'N'},
                {'N', 'N', 'N', 'Y', 'N', 'Y'},
                {'N', 'N', 'N', 'N', 'Y', 'Y'},
        });
        ALL_PATTERNS.put("Half-Bakery", new char[][]{
                {'N', 'Y', 'Y', 'N', 'N', 'N', 'N'},
                {'Y', 'N', 'N', 'Y', 'N', 'N', 'N'},
                {'N', 'Y', 'N', 'Y', 'N', 'N', 'N'},
                {'N', 'N', 'Y', 'N', 'Y', 'Y', 'N'},
                {'N', 'N', 'N', 'Y', 'N', 'N', 'Y'},
                {'N', 'N', 'N', 'N', 'Y', 'N', 'Y'},
                {'N', 'N', 'N', 'N', 'N', 'Y', 'N'}
        });
        ALL_PATTERNS.put("Gosper Glider Gun", new char[][]{
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'Y'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'Y', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'Y'},
                {'Y', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'Y', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'Y', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'Y', 'N', 'Y', 'Y', 'N', 'N', 'N', 'N', 'Y', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
        });
        ALL_PATTERNS.put("Candelabra", new char[][]{
                {'N', 'N', 'N', 'N', 'Y', 'Y', 'N', 'N', 'N', 'N', 'Y', 'Y', 'N', 'N', 'N', 'N'},
                {'N', 'Y', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'Y', 'N'},
                {'Y', 'N', 'Y', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'Y', 'N', 'Y'},
                {'N', 'Y', 'N', 'N', 'Y', 'N', 'Y', 'Y', 'Y', 'Y', 'N', 'Y', 'N', 'N', 'Y', 'N'},
                {'N', 'N', 'N', 'N', 'Y', 'N', 'Y', 'N', 'N', 'Y', 'N', 'Y', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N'}
        });
        ALL_PATTERNS.put("Beaconmaker", new char[][]{
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'Y'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'Y'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'Y', 'Y', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'}
        });
        ALL_PATTERNS.put("Chemist", new char[][]{
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'Y', 'Y', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'Y', 'Y', 'Y', 'N', 'N', 'Y', 'N', 'N', 'Y', 'Y'},
                {'N', 'N', 'N', 'N', 'Y', 'N', 'Y', 'N', 'Y', 'N', 'Y', 'N', 'Y', 'N', 'Y'},
                {'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'Y', 'N', 'Y', 'N', 'Y', 'N', 'N'},
                {'N', 'Y', 'Y', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'Y', 'Y', 'N'},
                {'N', 'N', 'Y', 'N', 'Y', 'N', 'Y', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N'},
                {'Y', 'N', 'Y', 'N', 'Y', 'N', 'Y', 'N', 'Y', 'N', 'Y', 'N', 'N', 'N', 'N'},
                {'Y', 'Y', 'N', 'N', 'Y', 'N', 'N', 'Y', 'Y', 'Y', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'Y', 'Y', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
                {'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', 'N', 'N', 'N', 'N', 'N', 'N', 'N'},
        });
    }

    private Color getColor(Scanner data)
    {
        String[] pos = data.next().split(",");
        return new Color(Integer.parseInt(pos[0]), Integer.parseInt(pos[1]), Integer.parseInt(pos[2]));
    }

    private Dimension getDimension(Scanner data)
    {
        String[] pos = data.next().split(",");
        return new Dimension(Integer.parseInt(pos[0]), Integer.parseInt(pos[1]));
    }

    public int getSpeed()
    {
        return speed;
    }

    public void setSpeed(int speed)
    {
        this.speed = speed;
    }

    public int getSize()
    {
        return size;
    }

    public void setSize(int size)
    {
        this.size = size;
        gridSize = size * cellMultiple;
    }

    public void resetGeneration()
    {
        generation = 0;
    }

    public int incrementGeneration()
    {
        this.generation++;
        return generation;
    }

    public int getGeneration()
    {
        return generation;
    }

    public void setGeneration(int generation)
    {
        this.generation = generation;
    }

    public Cell[][] getGrid()
    {
        return grid;
    }

    public void clearGrid(int width, int height)
    {
        grid = new Cell[width][height];
    }

    public void initializeGrid()
    {
        for(int r = 0; r < grid.length; r++)
            for(int c = 0; c < grid[r].length; c++)
                grid[r][c] = new Cell();
    }

    public void setLiving(int x, int y, boolean living)
    {
        grid[x][y].setLiving(living);
    }

    public String getDefaultPattern()
    {
        return defaultPattern;
    }

    public void setDefaultPattern(String defaultPattern)
    {
        this.defaultPattern = defaultPattern;
    }

    public String getDefaultSpeed()
    {
        return defaultSpeed;
    }

    public void setDefaultSpeed(String defaultSpeed)
    {
        this.defaultSpeed = defaultSpeed;
    }

    public String getDefaultSize()
    {
        return defaultSize;
    }

    public void setDefaultSize(String defaultSize)
    {
        this.defaultSize = defaultSize;
    }

    public boolean getDefaultGridVisibility()
    {
        return defaultGridVisibility;
    }

    public void setDefaultGridVisibility(boolean defaultGridVisibility)
    {
        this.defaultGridVisibility = defaultGridVisibility;
    }

    public boolean gridVisible()
    {
        return showGrid;
    }

    public void setGridVisibility(boolean showGrid)
    {
        this.showGrid = showGrid;
    }

    public int getCamX()
    {
        return camX;
    }

    public void decrementCamX()
    {
        camX -= size;
    }

    public void incrementCamX()
    {
        camX += size;
    }

    public int getCamY()
    {
        return camY;
    }

    public void decrementCamY()
    {
        camY -= size;
    }

    public void incrementCamY()
    {
        camY += size;
    }

    public Color getDeadColor()
    {
        return deadColor;
    }

    public void setDeadColor(Color deadColor)
    {
        this.deadColor = deadColor;
    }

    public Color getLivingColor()
    {
        return livingColor;
    }

    public void setLivingColor(Color livingColor)
    {
        this.livingColor = livingColor;
    }

    public void setGridlineColor(Color gridlineColor)
    {
        this.gridlineColor = gridlineColor;
    }

    public Color getBackgroundColor()
    {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }

    public Color getGridlineColor()
    {
        return gridlineColor;
    }

    public Color getDefaultDeadColor()
    {
        return defaultDeadColor;
    }

    public void setDefaultDeadColor(Color defaultDeadColor)
    {
        this.defaultDeadColor = defaultDeadColor;
    }

    public Color getDefaultLivingColor()
    {
        return defaultLivingColor;
    }

    public void setDefaultLivingColor(Color defaultLivingColor)
    {
        this.defaultLivingColor = defaultLivingColor;
    }

    public Color getDefaultBackgroundColor()
    {
        return defaultBackgroundColor;
    }

    public void setDefaultBackgroundColor(Color defaultBackgroundColor)
    {
        this.defaultBackgroundColor = defaultBackgroundColor;
    }

    public Color getDefaultGridlineColor()
    {
        return defaultGridlineColor;
    }

    public void setDefaultGridlineColor(Color defaultGridlineColor)
    {
        this.defaultGridlineColor = defaultGridlineColor;
    }

    public int getGridSize()
    {
        return gridSize;
    }

    public String[] getPatterns()
    {
        String[] s = new String[]{};
        ArrayList<String> list = Collections.list(ALL_PATTERNS.keys());
        Collections.sort(list);
        list.remove("Clear");
        list.add(0, "Clear");
        return list.toArray(s);
    }

    public Dictionary getPatternDictionary()
    {
        return ALL_PATTERNS;
    }

    public String[] getSpeeds()
    {
        return ALL_SPEEDS;
    }

    public String[] getSizes()
    {
        return ALL_SIZES;
    }

    public Dimension getFrameSize()
    {
        return frameSize;
    }
}

class View extends JFrame
{
    private final GamePanel gamePanel;
    private final JPanel optionsPanel = new JPanel(new GridLayout(2, 4, 5, 5));
    private final JCheckBox edit = new JCheckBox("Edit Mode");
    private final JCheckBox[] showGrid = new JCheckBox[2];
    private final JCheckBoxMenuItem editOnStart = new JCheckBoxMenuItem("Allow Edit on Start");
    private final JButton next = new JButton("Next");
    private final JButton startStop = new JButton("Start");
    private final JSlider speedSlider;
    private final JSlider sizeSlider;
    private final JComboBox patternBox = new JComboBox<>();
    private final JLabel generation = new JLabel();
    private final JMenu file;
    private final JMenu preferences;
    private final JMenu options;
    private final JMenu help;
    private final JMenuItem[] helpMenu;
    private final JMenuItem[] aboutMenu;
    private final JMenuItem[] saveMenu;
    private final JMenuItem[] loadMenu;
    private final JMenuItem shortcutMenu = new JMenuItem("Shortcuts");
    private final JMenuItem quitMenu = new JMenuItem("Quit");
    private final JMenuItem[] backgroundColorMenu;
    private final JMenuItem[] deadColorMenu;
    private final JMenuItem[] livingColorMenu;
    private final JMenuItem[] gridlineColorMenu;
    private final JRadioButtonMenuItem[] patternRadio;
    private final JRadioButtonMenuItem[] speedRadio;
    private final JRadioButtonMenuItem[] sizeRadio;
    private final JCheckBox showMenu = new JCheckBox("Show Menu");
    private final JMenuBar menuBar = new JMenuBar();
    private final JPopupMenu popup = new JPopupMenu();

    public View(Model model)
    {
        setTitle("Game of Life");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        helpMenu = initializeJMenuItems("Help");
        aboutMenu = initializeJMenuItems("About");
        saveMenu = initializeJMenuItems("Save");
        loadMenu = initializeJMenuItems("Load");
        backgroundColorMenu = initializeJMenuItems("Background Color");
        deadColorMenu = initializeJMenuItems("Dead Cell Color");
        livingColorMenu = initializeJMenuItems("Living Cell Color");
        gridlineColorMenu = initializeJMenuItems("Gridline Color");
        for(int i = 0; i < showGrid.length; i++)
            showGrid[i] = new JCheckBox("Show Gridlines");
        file = addMenu("File", 'F', loadMenu[0], saveMenu[0], quitMenu);
        menuBar.add(file);
        JMenu patternMenu = new JMenu("Shapes");
        JMenu speedMenu = new JMenu("Speed");
        JMenu sizeMenu = new JMenu("Scale");
        preferences = addMenu("Preferences", 'P', patternMenu, speedMenu, sizeMenu);
        preferences.addSeparator();
        addComponents(preferences, backgroundColorMenu[0], deadColorMenu[0], livingColorMenu[0], gridlineColorMenu[0]);
        preferences.addSeparator();
        preferences.add(showGrid[1]);
        menuBar.add(preferences);
        patternRadio = addMenuOption(model.getPatterns(), patternMenu);
        speedRadio = addMenuOption(model.getSpeeds(), speedMenu);
        sizeRadio = addMenuOption(model.getSizes(), sizeMenu);
        options = addMenu("Options", 'O', backgroundColorMenu[1], deadColorMenu[1], livingColorMenu[1], gridlineColorMenu[1]);
        options.addSeparator();
        options.add(editOnStart);
        menuBar.add(options);
        help = addMenu("Help", 'H', helpMenu[0], shortcutMenu, aboutMenu[0]);
        menuBar.add(help);
        menuBar.setVisible(false);
        menuBar.setBackground(model.getBackgroundColor());
        setJMenuBar(menuBar);

        gamePanel = new GamePanel(model);
        addComponents(popup, saveMenu[1], loadMenu[1]);
        popup.addSeparator();
        addComponents(popup, helpMenu[1], aboutMenu[1]);
        popup.addSeparator();
        popup.add(showMenu);
        gamePanel.add(popup);
        add(gamePanel, BorderLayout.CENTER);

        optionsPanel.setBackground(model.getBackgroundColor());
        optionsPanel.setFocusable(true);
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        edit.setBackground(model.getBackgroundColor());
        showGrid[0].setSelected(true);
        showGrid[0].setBackground(model.getBackgroundColor());
        for(String shape : model.getPatterns())
        {
            patternBox.addItem(shape);
        }
        patternBox.setEditable(false);
        patternBox.setSelectedItem(model.getDefaultPattern());
        speedSlider = addSlider(50, 800, 1550, 250, 750, model.getDefaultSpeed(), "Slow", "Normal", "Fast", model.getDefaultBackgroundColor());
        sizeSlider = addSlider(5, 20, 35, 5, 15, model.getDefaultSize(), "Small", "Medium", "Big", model.getDefaultBackgroundColor());
        addComponents(optionsPanel, edit, patternBox, speedSlider, next, showGrid[0], generation, sizeSlider, startStop);
        showGrid[0].setSelected(model.gridVisible());
        add(optionsPanel, BorderLayout.SOUTH);
        setMinimumSize(new Dimension(750, 400));
        setSize(model.getFrameSize());
        setLocationRelativeTo(null);
        new SplashScreen(this);
        setVisible(true);
    }

    private JMenuItem[] initializeJMenuItems(String text)
    {
        JMenuItem[] items = new JMenuItem[2];
        for(int i = 0; i < items.length; i++)
            items[i] = new JMenuItem(text);
        return items;
    }

    private JMenu addMenu(String name, char mnemonic, JMenuItem... items)
    {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        for(JMenuItem item : items)
            menu.add(item);
        return menu;
    }

    private JRadioButtonMenuItem[] addMenuOption(String[] options, JMenu menu)
    {
        JRadioButtonMenuItem[] items = new JRadioButtonMenuItem[options.length];
        ButtonGroup group = new ButtonGroup();
        for(int i = 0; i < options.length; i++)
        {
            items[i] = new JRadioButtonMenuItem(options[i]);
            group.add(items[i]);
            menu.add(items[i]);
        }
        return items;
    }

    private JSlider addSlider(int minimum, int middle, int maximum, int minor, int major, String defaultVal, String min, String mid, String max, Color background)
    {
        JSlider slider = new JSlider();
        slider.setBackground(background);
        slider.setPaintTicks(true);
        slider.setMinimum(minimum);
        slider.setMaximum(maximum);
        slider.setMajorTickSpacing(major);
        slider.setMinorTickSpacing(minor);
        slider.setSnapToTicks(false);
        slider.setPaintLabels(true);
        if(defaultVal.equals(min))
            slider.setValue(minimum);
        else if(defaultVal.equals(max))
            slider.setValue(maximum);
        else
            slider.setValue(middle);
        Dictionary<Integer, Component> labelTable = new Hashtable<>();
        labelTable.put(minimum, new JLabel(min));
        labelTable.put(middle, new JLabel(mid));
        labelTable.put(maximum, new JLabel(max));
        slider.setLabelTable(labelTable);
        return slider;
    }

    private void addComponents(JComponent parent, JComponent... items)
    {
        for(JComponent item : items)
            parent.add(item);
    }

    public String saveFile()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("SavedData.txt"));
        chooser.setFileFilter(new FileNameExtensionFilter("TXT File", "txt"));
        if(chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile().toString();
        else
            return "";
    }

    public String loadFile()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("TXT File", "txt"));
        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile().toString();
        else
            return "";
    }

    public void showError(Exception e)
    {
        JOptionPane.showMessageDialog(this, "File is invalid:\n\n" + e.toString(), "Error opening file", JOptionPane.ERROR_MESSAGE);
    }

    public void showHelpAboutShortcuts(int help)
    {
        String contents;
        JDialog dialog = new JDialog(this);
        if(help == 0)
        {
            contents =
                    """
                       <html><body>
                       <p style="text-align:center;"> <font face='Arial' size="+5"> <b> Help </b> </font> </p>
                       <p style="text-align:left;"> <font face='Arial'size="-1"> <br>\s
                       Every cell is in one of two possible states: <i>live</i> or <i>dead</i>.<br>
                       They interact with their <i>eight neighboring cells</i> to determine their next state.<br>
                       There are <u><i>three laws</i></u> in Conway's Game of Life:<br>
                       <font color='blue'><b>1. Any live cell with two or three live neighbours survives.<br></b></font>
                       <font color='green'><b>2. Any dead cell with three live neighbours becomes a live cell.<br></b></font>
                       <font color='red'><b>3. All other live cells die in the next generation. Similarly, all other dead cells stay dead.<br></b></font>
                       Through these simple rules, generations of cells can form complex patterns and shapes which <br>
                       symbolize the evolution of life.<br><br>
                       The <u>edit checkbox</u> allows you to toggle cells alive by pressing and holding down your left mouse button.<br>
                       Similarly you can toggle cells dead by pressing and holding down your right mouse button.<br>
                       The <u>show gridlines checkbox</u> allows you to toggle the gridlines visible or invisible.<br><br>
                       The <u>shapes combo-box</u> allows you to choose your initial pattern, it also allows you to clear the grid and restart.<br>
                       Clicking the <u>next button</u> allows you to progress by a single generation.<br>
                       Clicking the <u>start button</u> will begin simulating through generations at your preferred speed.<br>
                       Alternatively, clicking the <u>stop button</u> will stop/pause the simulation.<br><br>
                       The <u>speed slider</u> allows you to adjust the speed at which the simulation progresses.<br>
                       The <u>scale slider</u> allows you to zoom in and out in order to see the cells more clearly or to see the entire grid.<br><br>
                       The <u>generation counter</u> tells you the generation that the simulation is currently in.<br><br>
                       The <u>pop-up menu</u> is accessible by right-clicking on the grid when editing is toggled off.<br>
                       <u>Save</u> and <u>load options</u> are available in the pop-up menu, allowing you to save and load files in your computer.<br><br>
                       You can press, hold and drag your mouse to <u>move around the grid</u>.<br><br>
                       You can also toggle the <u>menu bar</u> to be shown at the top of the grid.<br>
                       In this menu bar, you can set your <u>preferences</u> for your next game start-ups.<br>
                       You also have the option to save and load through the menu bar.<br>
                       There are more options such as <u>color selection</u> for different aspects of the game and other preferences. <br>
                       You can also <u>enable and disable editing while in auto mode</u> through the Options tab in the menu bar.<br><br>
                       <u>Help</u> and <u>about options</u> are available in the pop-up menu and the menu bar for more information on<br>
                       Conway's Game of Life and this adaptation of it.<br><br>
                       You also have the option to <u>quit</u> through the menu bar.<br>
                       The program <u>automatically saves the window size</u> when you exit it for your next start-up. <br><br>
                       </font> </p>
                       <p style="text-align:center;"> <font face='Arial'size="+1"> <br>\s
                       <font color='blue'><b><u>Have fun and enjoy the brilliance of this automaton!!</u></b><br><br></font>
                       </font> </p>
                       </body></html>""";
            dialog.setTitle("Help");
        } else if(help == 1)
        {
            contents =
                    """
                            <html><body>
                            <p style="text-align:center;"> <font face='Arial' size="+5"> <b> About </b> </font> </p>
                            <p style="text-align:center;"> <font face='Arial'size="+0"> <br>\s
                            Conway's Game of Life, also known simply as 'Life',<br>
                            is a cellular automaton devised by mathematician <br>
                            <b>John Horton Conway in 1970.</b> <br>
                            It is a <font color='red'>zero-player</font> game, <br>
                            meaning that its evolution is determined by its <br>
                            initial state, requiring no further input. <br><br>
                            The universe of the Game of Life is an <font color='blue'>infinite,<br>
                            two-dimensional orthogonal grid of squares</font>,<br>
                            each of which is in one of <font color='green'>two possible states:<br>
                            <i><b>live</b></i> or <i><b>dead</b></i>. <br> </font>
                            Every cell interacts with <font color='orange'><u><b>eight</b> neighboring cells</u></font> which<br>
                            determine their next state.<br> <br>
                            The initial pattern constitutes the seed of the system.<br>
                            The first generation is created by applying the above <br>
                            rules simultaneously to every cell in the seed, live or dead.<br>
                            Births and deaths occur simultaneously, and the discrete <br>
                            moment at which this happens is sometimes called a <i>tick</i>. <br>
                            Each generation is a pure function of the preceding one. <br>
                            The rules continue to be applied repeatedly to create further <br>
                            generations. <br><br>
                            </font> </p></body></html>""";
            dialog.setTitle("About");
        } else
        {
            contents = """
                    <html><body>
                    <p style="text-align:left;"> <font face='Arial' size="+1"> <b> <br>Keyboard Shortcuts: </b> </font> </p>
                    <p style="text-align:left;"> <font face='Arial'size="+0"> <br>\s
                    <u>H</u> = Help Window <br>
                    <u>A</u> = About Window <br>
                    <u>K</u> = Shortcut Window <br><br>
                    <u>Spacebar</u> = Next <br>
                    <u>Enter</u> = Start/Stop <br><br>
                    <u>M</u> = Toggle Menu Bar <br>
                    <u>E</u> = Toggle Edit <br>
                    <u>G</u> = Toggle Gridlines <br><br>
                    <u>S</u> = Save <br>
                    <u>L</u> = Load <br><br>
                    </font> </p></body></html>""";
            dialog.setTitle("Shortcuts");
        }
        JLabel htmlContents = new JLabel(contents);
        htmlContents.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        dialog.add(htmlContents);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    public boolean canEdit()
    {
        return edit.isSelected();
    }

    public boolean gridVisible()
    {
        return showGrid[0].isSelected();
    }

    public void toggleGrid(boolean on)
    {
        showGrid[0].setSelected(!on);
    }

    public void toggleEdit(boolean on)
    {
        edit.setSelected(!on);
    }

    public void addGridListener(ActionListener a)
    {
        showGrid[0].addActionListener(a);
    }

    public void addGridPrefListener(ActionListener a)
    {
        showGrid[1].addActionListener(a);
    }

    public boolean gridPrefSelected()
    {
        return showGrid[1].isSelected();
    }

    public void addNextListener(ActionListener a)
    {
        next.addActionListener(a);
    }

    public void addStartStopListener(ActionListener a)
    {
        startStop.addActionListener(a);
    }

    public void addSpeedSliderListener(ChangeListener a)
    {
        speedSlider.addChangeListener(a);
    }

    public void addSizeSliderListener(ChangeListener a)
    {
        sizeSlider.addChangeListener(a);
    }

    public void addSaveListener(ActionListener a)
    {
        for(JMenuItem save : saveMenu)
            save.addActionListener(a);
    }

    public void addLoadListener(ActionListener a)
    {
        for(JMenuItem load : loadMenu)
            load.addActionListener(a);
    }

    public void addPatternActionListener(ActionListener a)
    {
        patternBox.addActionListener(a);
    }

    public String getSelectedPattern()
    {
        return patternBox.getSelectedItem().toString();
    }

    public void addMenuBarListener(ActionListener a)
    {
        showMenu.addActionListener(a);
    }

    public void addQuitListener(ActionListener a)
    {
        quitMenu.addActionListener(a);
    }

    public void addColorPrefListener(String colorChoice, ActionListener a)
    {
        switch(colorChoice)
        {
            case "GRIDLINE":
                gridlineColorMenu[0].addActionListener(a);
                break;
            case "DEAD":
                deadColorMenu[0].addActionListener(a);
                break;
            case "LIVING":
                livingColorMenu[0].addActionListener(a);
                break;
            default:
                backgroundColorMenu[0].addActionListener(a);
        }
    }

    public void addOptionsMouseListener(MouseAdapter a)
    {
        optionsPanel.addMouseListener(a);
    }

    public void focusOnOptionsPanel()
    {
        optionsPanel.requestFocusInWindow();
    }

    public void focusOnGamePanel()
    {
        gamePanel.requestFocusInWindow();
    }

    public void addColorListener(String colorChoice, ActionListener a)
    {
        switch(colorChoice)
        {
            case "GRIDLINE":
                gridlineColorMenu[1].addActionListener(a);
                break;
            case "DEAD":
                deadColorMenu[1].addActionListener(a);
                break;
            case "LIVING":
                livingColorMenu[1].addActionListener(a);
                break;
            default:
                backgroundColorMenu[1].addActionListener(a);
        }
    }

    public void changeBackground(Color background, Color foreground)
    {
        menuBar.setBackground(background);
        optionsPanel.setBackground(background);
        edit.setBackground(background);
        showGrid[0].setBackground(background);
        speedSlider.setBackground(background);
        sizeSlider.setBackground(background);
        edit.setForeground(foreground);
        showGrid[0].setForeground(foreground);
        speedSlider.setForeground(foreground);
        sizeSlider.setForeground(foreground);
        file.setForeground(foreground);
        preferences.setForeground(foreground);
        options.setForeground(foreground);
        help.setForeground(foreground);
        generation.setForeground(foreground);
    }

    public void addPatternPrefActionListener(int index, ActionListener a)
    {
        patternRadio[index].addActionListener(a);
    }

    public void addSpeedPrefActionListener(int index, ActionListener a)
    {
        speedRadio[index].addActionListener(a);
    }

    public void addSizePrefActionListener(int index, ActionListener a)
    {
        sizeRadio[index].addActionListener(a);
    }

    public String getPatternPrefText(int index)
    {
        return patternRadio[index].getText();
    }

    public String getSpeedPrefText(int index)
    {
        return speedRadio[index].getText();
    }

    public String getSizePrefText(int index)
    {
        return sizeRadio[index].getText();
    }

    public void addHelpMenuActionListener(ActionListener a)
    {
        for(JMenuItem help : helpMenu)
            help.addActionListener(a);
    }

    public void addAboutMenuActionListener(ActionListener a)
    {
        for(JMenuItem about : aboutMenu)
            about.addActionListener(a);
    }

    public void addShortcutMenuActionListener(ActionListener a)
    {
        shortcutMenu.addActionListener(a);
    }

    public void addGameMouseListener(MouseAdapter a)
    {
        gamePanel.addMouseListener(a);
    }

    public void addGameMouseMotionListener(MouseMotionAdapter a)
    {
        gamePanel.addMouseMotionListener(a);
    }

    public void addGameKeyListener(KeyAdapter a)
    {
        gamePanel.addKeyListener(a);
    }

    public void addOptionsKeyListener(KeyAdapter a)
    {
        optionsPanel.addKeyListener(a);
    }

    public void showPopUpMenu(MouseEvent e)
    {
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    public void changeStartStopButton(boolean isStarted)
    {
        if(isStarted)
        {
            startStop.setText("Stop");
            next.setEnabled(false);
            if(!editOnStart.isSelected())
            {
                edit.setSelected(false);
                edit.setEnabled(false);
            }
        }
        else
        {
            startStop.setText("Start");
            next.setEnabled(true);
            edit.setEnabled(true);
        }
    }

    public void setMenuBarVisibility(boolean isVisible)
    {
        menuBar.setVisible(isVisible);
    }

    public boolean getMenuBarSelection()
    {
        return showMenu.isSelected();
    }

    public void toggleMenuBar(boolean on)
    {
        showMenu.setSelected(!on);
    }

    public void updateGenerationCounter(int generationVal)
    {
        generation.setText("Generation: " + generationVal);
    }

    public void changeCursor(boolean isDragging, boolean editMode)
    {
        if(isDragging)
        {
            if(editMode)
                gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            else
                gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        else
            gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void repaintGamePanel()
    {
        gamePanel.repaint();
    }

}

class SplashScreen extends JDialog
{
    private BufferedImage image;

    public SplashScreen(JFrame frame)
    {
        super(frame, "Game of Life", true);
        setSize(500, 500);
        setUndecorated(true);
        setLocationRelativeTo(null);
        try
        {
            image = ImageIO.read(getClass().getResource("splashscreen.jpg"));
        } catch(IOException | IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        add(new JComponent()
        {
            @Override
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, this);
            }
        });
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                dispose();
            }
        });
        setVisible(true);
    }
}

class Controller
{
    private final Model model;
    private final View view;
    private boolean toggleAlive = false;
    private boolean toggleDead = false;
    private boolean isStarted = false;

    private Point clickedPos = new Point();

    public Controller(Model gameModel, View gameView)
    {
        model = gameModel;
        view = gameView;
        resetGeneration();
        view.addGridListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                model.setGridVisibility(view.gridVisible());
                view.repaintGamePanel();
            }
        });
        view.addGridPrefListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                model.setDefaultGridVisibility(view.gridPrefSelected());
            }
        });
        view.addNextListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                simulateGeneration();
            }
        });
        view.addStartStopListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                startStopGeneration();
            }
        });
        view.addSaveListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                saveProgress();
            }
        });
        view.addLoadListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                loadProgress();
            }
        });
        view.addQuitListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                System.exit(0);
            }
        });
        view.addMenuBarListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                view.setMenuBarVisibility(view.getMenuBarSelection());
            }
        });
        view.addSpeedSliderListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                JSlider source = (JSlider) e.getSource();
                int speed = (source.getMaximum() + source.getMinimum()) - source.getValue();
                model.setSpeed(speed);
            }
        });
        view.addSizeSliderListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                JSlider source = (JSlider) e.getSource();
                model.setSize(source.getValue());
                view.repaintGamePanel();
            }
        });
        view.addPatternActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if(view.getSelectedPattern().equals("Clear"))
                {
                    model.initializeGrid();
                    resetGeneration();
                } else
                {
                    addPattern((char[][]) model.getPatternDictionary().get(view.getSelectedPattern()));
                }
                view.repaintGamePanel();
            }
        });
        for(int i = 0; i < model.getPatterns().length; i++)
        {
            int finalI = i;
            view.addPatternPrefActionListener(finalI, new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    model.setDefaultPattern(view.getPatternPrefText(finalI));
                }
            });
        }
        for(int i = 0; i < model.getSpeeds().length; i++)
        {
            int finalI = i;
            view.addSpeedPrefActionListener(finalI, new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    model.setDefaultSpeed(view.getSpeedPrefText(finalI));
                }
            });
        }
        for(int i = 0; i < model.getSizes().length; i++)
        {
            int finalI = i;
            view.addSizePrefActionListener(finalI, new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    model.setDefaultSize(view.getSizePrefText(finalI));
                }
            });
        }
        view.addHelpMenuActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                view.showHelpAboutShortcuts(0);
            }
        });
        view.addAboutMenuActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                view.showHelpAboutShortcuts(1);
            }
        });
        view.addShortcutMenuActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                view.showHelpAboutShortcuts(2);
            }
        });
        view.addColorPrefListener("BACKGROUND", new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Color background = JColorChooser.showDialog(view, "Choose Default Background Color", model.getDefaultBackgroundColor());
                if(background == null)
                    background = model.getDefaultBackgroundColor();
                model.setDefaultBackgroundColor(background);
            }
        });
        view.addColorPrefListener("DEAD", new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Color background = JColorChooser.showDialog(view, "Choose Default Dead Cell Color", model.getDefaultDeadColor());
                if(background == null)
                    background = model.getDefaultDeadColor();
                model.setDefaultDeadColor(background);
            }
        });
        view.addColorPrefListener("LIVING", new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Color background = JColorChooser.showDialog(view, "Choose Default Living Cell Color", model.getDefaultLivingColor());
                if(background == null)
                    background = model.getDefaultLivingColor();
                model.setDefaultLivingColor(background);
            }
        });
        view.addColorPrefListener("GRIDLINE", new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Color background = JColorChooser.showDialog(view, "Choose Default Gridline Color", model.getDefaultGridlineColor());
                if(background == null)
                    background = model.getDefaultGridlineColor();
                model.setDefaultGridlineColor(background);
            }
        });
        view.addColorListener("BACKGROUND", new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Color background = JColorChooser.showDialog(view, "Choose Background Color", model.getBackgroundColor());
                if(background == null)
                    background = model.getBackgroundColor();
                model.setBackgroundColor(background);
                view.changeBackground(background, invertColor(background));
                view.repaintGamePanel();
            }
        });
        view.addColorListener("DEAD", new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Color background = JColorChooser.showDialog(view, "Choose Dead Cell Color", model.getDeadColor());
                if(background == null)
                    background = model.getDeadColor();
                model.setDeadColor(background);
                view.repaintGamePanel();
            }
        });
        view.addColorListener("LIVING", new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Color background = JColorChooser.showDialog(view, "Choose Living Cell Color", model.getLivingColor());
                if(background == null)
                    background = model.getLivingColor();
                model.setLivingColor(background);
                view.repaintGamePanel();
            }
        });
        view.addColorListener("GRIDLINE", new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Color background = JColorChooser.showDialog(view, "Choose Gridline Cell Color", model.getGridlineColor());
                if(background == null)
                    background = model.getGridlineColor();
                model.setGridlineColor(background);
                view.repaintGamePanel();
            }
        });
        view.addGameMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(e.getButton() == MouseEvent.BUTTON1)
                {
                    mouseAction(model, view, e, true);
                }
                if(e.getButton() == MouseEvent.BUTTON3)
                {
                    mouseAction(model, view, e, false);
                }
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                view.focusOnGamePanel();
                clickedPos = e.getPoint();
                if(e.getButton() == MouseEvent.BUTTON1)
                {
                    toggleAlive = true;
                }
                if(e.getButton() == MouseEvent.BUTTON3)
                {
                    toggleDead = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                view.changeCursor(false, false);
                if(e.getButton() == MouseEvent.BUTTON1)
                {
                    toggleAlive = false;
                }
                if(e.getButton() == MouseEvent.BUTTON3)
                {
                    toggleDead = false;
                    if(!view.canEdit())
                        view.showPopUpMenu(e);
                }
            }
        });
        view.addGameMouseMotionListener(new MouseMotionAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                if(toggleAlive)
                    mouseAction(model, view, e, true);
                if(toggleDead)
                    mouseAction(model, view, e, false);
                if(!view.canEdit())
                {
                    view.changeCursor(true, false);
                    if(clickedPos.x > e.getX())
                        model.decrementCamX();
                    if(clickedPos.x < e.getX())
                        model.incrementCamX();
                    if(clickedPos.y > e.getY())
                        model.decrementCamY();
                    if(clickedPos.y < e.getY())
                        model.incrementCamY();
                    view.repaintGamePanel();
                } else
                    view.changeCursor(true, true);
                clickedPos.x = e.getX();
                clickedPos.y = e.getY();
            }
        });
        view.addOptionsMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                view.focusOnOptionsPanel();
            }
        });
        view.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                try
                {
                    PrintWriter save = new PrintWriter("preferences.txt");
                    save.println(model.getDefaultPattern());
                    save.println(model.getDefaultSpeed());
                    save.println(model.getDefaultSize());
                    save.println(convertColorToString(model.getDefaultBackgroundColor()));
                    save.println(convertColorToString(model.getDefaultDeadColor()));
                    save.println(convertColorToString(model.getDefaultLivingColor()));
                    save.println(convertColorToString(model.getDefaultGridlineColor()));
                    save.println(view.getSize().width + "," + view.getSize().height);
                    save.print(model.getDefaultGridVisibility());
                    save.close();
                } catch(FileNotFoundException ex) {}
            }
        });
        KeyController keys = new KeyController(model, view, this);
        view.addGameKeyListener(keys);
        view.addOptionsKeyListener(keys);
    }

    public void saveProgress()
    {
        StringBuilder contents = new StringBuilder(model.getGeneration() + "\n");
        for(int r = 0; r < model.getGrid().length; r++)
        {
            for(int c = 0; c < model.getGrid()[r].length; c++)
            {
                if(model.getGrid()[r][c].isLiving())
                    contents.append(r).append(",").append(c).append("\n");
            }
        }
        String saveDirectory = view.saveFile();
        if(!saveDirectory.equals(""))
        {
            try
            {
                PrintWriter save = new PrintWriter(saveDirectory);
                save.print(contents);
                save.close();
            } catch(FileNotFoundException ex) {}
        }
    }

    public void loadProgress()
    {
        String loadDirectory = view.loadFile();
        if(!loadDirectory.equals(""))
        {
            try
            {
                Scanner data = new Scanner(new File(loadDirectory));
                model.setGeneration(data.nextInt());
                view.updateGenerationCounter(model.getGeneration());
                model.initializeGrid();
                while(data.hasNext())
                {
                    String[] pos = data.next().split(",");
                    model.setLiving(Integer.parseInt(pos[0]), Integer.parseInt(pos[1]), true);
                }
                data.close();
                view.repaintGamePanel();
            } catch(Exception ex)
            {
                view.showError(ex);
            }
        }
    }

    public void startStopGeneration()
    {
        if(!isStarted)
        {
            isStarted = true;
            Thread simulateThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    while(isStarted)
                    {
                        try
                        {
                            simulateGeneration();
                            Thread.sleep(model.getSpeed());
                        } catch(InterruptedException e)
                        {
                        }
                    }
                }
            });
            simulateThread.start();
            view.changeStartStopButton(true);
        }
        else
        {
            isStarted = false;
            view.changeStartStopButton(false);
        }
    }

    private void resetGeneration()
    {
        model.resetGeneration();
        view.updateGenerationCounter(0);
    }

    public void simulateGeneration()
    {
        calculateNeighbours();
        calculateNextState();
        simulateNextState();
        view.updateGenerationCounter(model.incrementGeneration());
        view.repaintGamePanel();
    }

    private void calculateNeighbours()
    {
        for(int r = 0; r < model.getGrid().length; r++)
        {
            for(int c = 0; c < model.getGrid()[r].length; c++)
            {
                if(r == model.getGrid().length - 1 && c == model.getGrid().length - 1)
                {
                    if(model.getGrid()[r - 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][0].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][0].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[0][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[0][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[0][0].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                }
                else if(r == 0 && c == 0)
                {
                    if(model.getGrid()[r + 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][model.getGrid()[r].length - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][model.getGrid()[r].length - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[model.getGrid().length - 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[model.getGrid().length - 1][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[model.getGrid().length - 1][model.getGrid()[r].length - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                }
                else if(r == 0 && c == model.getGrid().length - 1)
                {
                    if(model.getGrid()[r][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][0].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][0].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[model.getGrid().length - 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[model.getGrid().length - 1][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[model.getGrid().length - 1][0].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                }
                else if(r == model.getGrid().length - 1 && c == 0)
                {
                    if(model.getGrid()[r - 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][model.getGrid()[r].length - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][model.getGrid()[r].length - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[0][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[0][0].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[0][model.getGrid()[r].length - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                }
                else if(r == model.getGrid().length - 1)
                {
                    if(model.getGrid()[r][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[0][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[0][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[0][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                }
                else if(c == model.getGrid().length - 1)
                {
                    if(model.getGrid()[r + 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][0].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][0].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][0].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                }
                else if(r == 0)
                {
                    if(model.getGrid()[r][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[model.getGrid().length - 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[model.getGrid().length - 1][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[model.getGrid().length - 1][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                }
                else if(c == 0)
                {
                    if(model.getGrid()[r + 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][model.getGrid()[r].length - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][model.getGrid()[r].length - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][model.getGrid()[r].length - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                }
                else
                {
                    if(model.getGrid()[r + 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r - 1][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][c + 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                    if(model.getGrid()[r + 1][c - 1].isLiving())
                        model.getGrid()[r][c].incrementNeighbours();
                }
            }
        }
    }

    private void calculateNextState()
    {
        for(int r = 0; r < model.getGrid().length; r++)
        {
            for(int c = 0; c < model.getGrid()[r].length; c++)
            {
                if(model.getGrid()[r][c].isLiving())
                {
                    if(model.getGrid()[r][c].getNeighbours() == 2 || model.getGrid()[r][c].getNeighbours() == 3)
                        model.getGrid()[r][c].setNextState(true);
                } else
                {
                    if(model.getGrid()[r][c].getNeighbours() == 3)
                        model.getGrid()[r][c].setNextState(true);
                }
            }
        }
    }

    private void simulateNextState()
    {
        for(int r = 0; r < model.getGrid().length; r++)
        {
            for(int c = 0; c < model.getGrid()[r].length; c++)
            {
                model.getGrid()[r][c].setLiving(model.getGrid()[r][c].willLive());
                model.getGrid()[r][c].setNextState(false);
                model.getGrid()[r][c].resetNeighbours();
            }
        }
    }

    private void addPattern(char[][] pattern)
    {
        Point center = new Point(((model.getGrid().length - 1) / 2) - pattern.length / 2, (model.getGrid()[0].length - 1) / 2 - pattern[0].length / 2);
        for(int r = 0; r < pattern.length; r++)
        {
            for(int c = 0; c < pattern[r].length; c++)
            {
                if(pattern[r][c] == 'Y')
                    model.getGrid()[center.x + c][center.y + r].setLiving(true);
            }
        }
    }

    private void mouseAction(Model model, View view, MouseEvent e, boolean leftClick)
    {
        if(view.canEdit())
        {
            int x = e.getX() / model.getSize();
            int y = e.getY() / model.getSize();
            x -= model.getCamX() / model.getSize();
            y -= model.getCamY() / model.getSize();
            if(x < 0)
                x = 0;
            else if(x > model.getGrid().length - 1)
                x = model.getGrid().length - 1;
            if(y < 0)
                y = 0;
            else if(y > model.getGrid()[0].length - 1)
                y = model.getGrid()[0].length - 1;
            model.setLiving(x, y, leftClick);
            view.repaintGamePanel();
        }
    }

    private Color invertColor(Color background)
    {
        double rgb = (background.getRed() * 0.299) + (background.getGreen() * 0.587) + (background.getBlue() * 0.114);
        if(rgb > 186)
            return Color.BLACK;
        else
            return Color.WHITE;
    }

    private String convertColorToString(Color color)
    {
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }
}

class KeyController extends KeyAdapter
{
    private final Model model;
    private final View view;
    private final Controller controller;
    public KeyController(Model model, View view, Controller controller)
    {
        this.model = model;
        this.view = view;
        this.controller = controller;
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_H)
            view.showHelpAboutShortcuts(0);
        if(e.getKeyCode() == KeyEvent.VK_A)
            view.showHelpAboutShortcuts(1);
        if(e.getKeyCode() == KeyEvent.VK_K)
            view.showHelpAboutShortcuts(2);
        if(e.getKeyCode() == KeyEvent.VK_SPACE)
            controller.simulateGeneration();
        if(e.getKeyCode() == KeyEvent.VK_ENTER)
            controller.startStopGeneration();
        if(e.getKeyCode() == KeyEvent.VK_G)
        {
            view.toggleGrid(model.gridVisible());
            model.setGridVisibility(view.gridVisible());
            view.repaintGamePanel();
        }
        if(e.getKeyCode() == KeyEvent.VK_E)
            view.toggleEdit(view.canEdit());
        if(e.getKeyCode() == KeyEvent.VK_M)
        {
            view.toggleMenuBar(view.getMenuBarSelection());
            view.setMenuBarVisibility(view.getMenuBarSelection());
        }
        if(e.getKeyCode() == KeyEvent.VK_S)
            controller.saveProgress();
        if(e.getKeyCode() == KeyEvent.VK_L)
            controller.loadProgress();
        view.repaintGamePanel();
    }
}
