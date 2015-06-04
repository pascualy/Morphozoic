// For conditions of distribution and use, see copyright notice in morphozoic.h

/*
 * Morphozoic driver.
 */

#ifdef WIN32
#ifndef _DEBUG
#pragma comment( linker, "/subsystem:\"windows\" /entry:\"mainCRTStartup\"" )
#endif
#include <windows.h>
#endif
#include "../gui/TimeUtils.h"
#include "../gui/EasyGL.h"
#include "morphozoic.h"
#include "../util/gettime.h"
#include "../util/camera.hpp"
#include "../util/frameRate.hpp"

#ifdef MORPHOZOIC_MAIN
char *Usage[] =
{
   (char *)"morphozoic\n",
   (char *)"      [-cycles <number of cycles>]\n",
   (char *)"      [-numCells <number of cells>]\n",
#ifdef THREADS
   (char *)"      [-numThreads <number of threads (default=1)>]\n",
#endif
   (char *)"      [-vesselRadius <vessel radius>]\n",
   (char *)"      [-randomSeed <random seed>]\n",
   (char *)"      [-load <load file name>]\n",
   (char *)"      [-save <save file name>]\n",
   (char *)"      [-noGraphics (turn off graphics)]\n",
   NULL
};

void printUsage()
{
   for (int i = 0; Usage[i] != NULL; i++)
   {
      fprintf(stderr, "%s", Usage[i]);
   }
}


#endif

// Organism.
int      NumCells  = DEFAULT_NUM_CELLS;
Organism *organism = NULL;

// Vessel.
float VesselRadius = DEFAULT_VESSEL_RADIUS;

#ifdef THREADS
// Threads.
int NumThreads = DEFAULT_NUM_THREADS;
#endif

// Run cycles.
int Cycles       = -1;
int CycleCounter = 0;

// Random numbers.
RANDOM RandomSeed  = INVALID_RANDOM;
Random *Randomizer = NULL;

// Files.
char *SaveFile = NULL;
char *LoadFile = NULL;

// Graphics window dimensions.
bool Graphics     = true;
int  WindowWidth  = WINDOW_WIDTH;
int  WindowHeight = WINDOW_HEIGHT;
int  GUIwidth     = (int)((float)WINDOW_WIDTH / 3.5f);
int  MainWindow;

// Viewports.
#define ORGANISM_VIEWPORT    0
#define CONTROLS_VIEWPORT    1
#define NUM_VIEWPORTS        2
struct ViewportDimension
{
   GLint   x, y;
   GLint   width, height;
   GLfloat aspect;
}
Viewports[NUM_VIEWPORTS];

// Organism view/movement.
#define FRUSTUM_ANGLE     60.0f
#define FRUSTUM_NEAR      0.01f
#define FRUSTUM_FAR       1000.0f
#define MOVEMENT_DELTA    0.5f
GLfloat ViewPosition[3] = { 0.0f, 0.0f, 0.0f };

// Camera.
Camera camera;

// Shading.
GLfloat WhiteLight[] = { 1.0f, 1.0f, 1.0f, 1.0f };
GLfloat BlackMaterial[] = { 0.0f, 0.0f, 0.0f, 1.0f };
GLfloat DimMaterial[] = { 0.05f, 0.05f, 0.05f, 1.0f };

// Frame rate management.
#define TARGET_FRAME_RATE    50.0f
FrameRate frameRate(TARGET_FRAME_RATE);

/*
 *  Available fonts:
 *  GLUT_BITMAP_8_BY_13
 *  GLUT_BITMAP_9_BY_15
 *  GLUT_BITMAP_TIMES_ROMAN_10
 *  GLUT_BITMAP_TIMES_ROMAN_24
 *  GLUT_BITMAP_HELVETICA_10
 *  GLUT_BITMAP_HELVETICA_12
 *  GLUT_BITMAP_HELVETICA_18
 */
#define FONT          GLUT_BITMAP_9_BY_15
#define LINE_SPACE    15

// Picking.
#define BUFSIZE       1024
GLuint selectBuf[BUFSIZE];
GLint  pickHits;
GLenum renderMode = GL_RENDER;
int    lastButton;
int    cursorX, cursorY;
void startPicking();

void processHits(GLint hits, GLuint buffer[], int sw);
void stopPicking();

int CurrentCellID = -1;

// 2D functions.
void helpInfo();
void draw2Dstring(GLfloat x, GLfloat y, void *font, char *string);
void enter2DMode(GLint width = 0, GLint height = 0), exit2DMode();

// Control help.
char *ControlHelp[] =
{
   (char *)"    Up arrow : Move up",
   (char *)"  Down arrow : Move down",
   (char *)"  Left arrow : Move left",
   (char *)" Right arrow : Move right",
   (char *)"  PgUp arrow : Move away",
   (char *)"PgDown arrow : Move closer",
   (char *)"  Left mouse : Select cell",
   (char *)"       b key : Change background",
   (char *)"       u key : Toggle wireframe view",
   (char *)"       v key : Change vessel view",
   NULL
};

// GUI components.
#define RUN_CONTROL     0
#define EDIT_CONTROL    1
#define FILE_CONTROL    2
class EventsHandler : public GUIEventListener
{
public:
   virtual void actionPerformed(GUIEvent& evt);
};
EventsHandler  handler;
GUIFrame       guiFrame;
GUITabbedPanel *controlPanel;
GUISlider      *delaySlider;
GUILabel       *statusText;
GUIPanel       *cyclePanel;
GUILabel       *cycleDisplay;
GUIComboBox    *deleteOp;
GUIComboBox    *moveOp;
GUISlider      *xSlider;
GUISlider      *ySlider;
GUISlider      *zSlider;
GUITextBox     *fileNameText;
GUIComboBox    *fileOp;
FPSCounter     counter;
GUILabel       *fpsDisplay;

// Modes.
typedef enum
{
   ORGANISM_MODE = 0, HELP_MODE = 1
}
MODE;
MODE  Mode       = ORGANISM_MODE;
float Delay      = MAX_DELAY;
TIME  LastTime;
bool  Step       = false;
bool  WireView   = false;
bool  Background = true;
typedef enum
{
   TRANSPARENT_VESSEL, WIRE_VESSEL, INVISIBLE_VESSEL
}
VESSEL_APPEARANCE;
VESSEL_APPEARANCE VesselAppearance = TRANSPARENT_VESSEL;
typedef enum
{
   MOVE_CAMERA, MOVE_CELL
}
MOVEMENT_MODE;
MOVEMENT_MODE MovementMode = MOVE_CAMERA;

// Draw organism.
void drawOrganism()
{
   if (WireView)
   {
      glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
   }
   else
   {
      glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
   }
   glPushMatrix();

   // Draw organism.
   organism->draw(true);

   // Highlight current cell.
   if ((renderMode == GL_RENDER) && (CurrentCellID != -1))
   {
      organism->getCell(CurrentCellID)->highlight();
   }

   // Draw vessel.
   if (renderMode == GL_RENDER)
   {
      if (VesselAppearance == TRANSPARENT_VESSEL)
      {
         glMaterialfv(GL_FRONT, GL_AMBIENT, DimMaterial);
         glMaterialfv(GL_FRONT, GL_DIFFUSE, BlackMaterial);
         glEnable(GL_BLEND);
         glDepthMask(GL_FALSE);
         glBlendFunc(GL_SRC_ALPHA, GL_ONE);
         glutSolidSphere(VesselRadius, 50, 50);
         glDepthMask(GL_TRUE);
         glDisable(GL_BLEND);
      }
      else if (VesselAppearance == WIRE_VESSEL)
      {
         glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
         glDisable(GL_LIGHTING);
         if (Background)
         {
            glColor3f(0.0f, 0.0f, 0.0f);
         }
         else
         {
            glColor3f(1.0f, 1.0f, 1.0f);
         }
         glutSolidSphere(VesselRadius, 50, 50);
         glEnable(GL_LIGHTING);
      }
   }

   glPopMatrix();
   glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
}


// Display the organism view.
void displayOrganismView(void)
{
   // Set viewport and frustum.
   glViewport(Viewports[ORGANISM_VIEWPORT].x, Viewports[ORGANISM_VIEWPORT].y,
              Viewports[ORGANISM_VIEWPORT].width, Viewports[ORGANISM_VIEWPORT].height);
   camera.setFrustum(FRUSTUM_ANGLE, Viewports[ORGANISM_VIEWPORT].aspect,
                     FRUSTUM_NEAR, FRUSTUM_FAR);

   // Clear background with a "skydome".
   if (Background && (renderMode == GL_RENDER))
   {
      glCullFace(GL_FRONT);
      glDisable(GL_LIGHTING);
      glColor3f(0.9f, 0.9f, 1.0f);
      glMatrixMode(GL_MODELVIEW);
      glPushMatrix();
      glTranslatef(ViewPosition[0], ViewPosition[1], ViewPosition[2]);
      glutSolidSphere((FRUSTUM_FAR / 2.0f) - (FRUSTUM_FAR * 0.05f), 50, 50);
      glPopMatrix();
      glEnable(GL_LIGHTING);
      glCullFace(GL_BACK);
   }

   if (Mode != HELP_MODE)
   {
      // Position camera.
      camera.resetSpacial();
      camera.setPosition(ViewPosition);
      camera.setYaw(180.0f);
      camera.place();

      // Rendering to select a cell?
      if (renderMode == GL_SELECT)
      {
         startPicking();
      }

      // Draw the organism.
      glPushMatrix();
      drawOrganism();
      glPopMatrix();

      if (renderMode == GL_SELECT)
      {
         stopPicking();
      }
   }

   // Show mode information.
   if (Background)
   {
      glColor3f(0.0f, 0.0f, 0.0f);
   }
   else
   {
      glColor3f(1.0f, 1.0f, 1.0f);
   }
   enter2DMode();
   if (Mode == HELP_MODE)
   {
      helpInfo();
   }
   draw2Dstring(5, 15, GLUT_BITMAP_HELVETICA_18, (char *)"Organism:");
   exit2DMode();
}


// Display the controls view.
void displayControls(void)
{
   int    vw, vh;
   GLint  viewport[4];
   char   str[100];
   string buf;
   Cell   *cell;

   // Set viewport.
   glViewport(Viewports[CONTROLS_VIEWPORT].x, Viewports[CONTROLS_VIEWPORT].y,
              Viewports[CONTROLS_VIEWPORT].width, Viewports[CONTROLS_VIEWPORT].height);
   glGetIntegerv(GL_VIEWPORT, viewport);
   vw = viewport[2];
   vh = viewport[3];

   // Show status.
   buf = "";
   if (CurrentCellID != -1)
   {
      cell = organism->getCell(CurrentCellID);
      assert(cell != NULL);
      sprintf(str, "Cell ID=%d\n", CurrentCellID);
      buf.append(str);
   }

   // Show cycle.
   if (Cycles >= 0)
   {
      cyclePanel->setVisible(true);
      sprintf(str, "Cycle: %d/%d", CycleCounter, Cycles);
      cycleDisplay->setLabelString(str);
   }
   else
   {
      cyclePanel->setVisible(false);
   }

   // Set frame rate.
   sprintf(str, "FPS: %.1f", frameRate.FPS);
   fpsDisplay->setLabelString(str);

   // Render the GUI.
   glColor3f(1.0f, 1.0f, 1.0f);
   enter2DMode(guiFrame.getWidth(), guiFrame.getHeight());
   counter.markFrameStart();
   guiFrame.render(counter.getFrameInterval());
   counter.markFrameEnd();
   exit2DMode();
}


// Draw window partitions.
void drawPartitions()
{
   glViewport(0, 0, WindowWidth, WindowHeight);
   enter2DMode();
   glLineWidth(2.0);
   glColor3f(1.0f, 1.0f, 1.0f);
   glBegin(GL_LINES);
   glVertex2f(Viewports[ORGANISM_VIEWPORT].width, 0.0f);
   glVertex2f(Viewports[ORGANISM_VIEWPORT].width, WindowHeight);
   glEnd();
   glLineWidth(1.0f);
   exit2DMode();
}


// Display function.
void display(void)
{
   // Clear display.
   glutSetWindow(MainWindow);
   glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

   // Normal rendering?
   if (renderMode == GL_RENDER)
   {
      // Display viewports.
      displayOrganismView();
      displayControls();
      drawPartitions();

      glutSwapBuffers();
      glFlush();

      // Update frame rate.
      frameRate.update();
   }
   else
   {
      // Rendering for cell selection.
      displayOrganismView();
   }
}


// Configure viewport dimensions.
void configureViewports()
{
   int i;

   i = ORGANISM_VIEWPORT;
   Viewports[i].x      = 0;
   Viewports[i].y      = 0;
   Viewports[i].width  = WindowWidth - GUIwidth;
   Viewports[i].height = WindowHeight;
   if (Viewports[i].height > 0)
   {
      Viewports[i].aspect = (GLfloat)Viewports[i].width / (GLfloat)Viewports[i].height;
   }
   else
   {
      Viewports[i].aspect = 1.0f;
   }
   i = CONTROLS_VIEWPORT;
   Viewports[i].x      = WindowWidth - GUIwidth;
   Viewports[i].y      = 0;
   Viewports[i].width  = GUIwidth;
   Viewports[i].height = WindowHeight;
   if (Viewports[i].height > 0)
   {
      Viewports[i].aspect = (GLfloat)Viewports[i].width / (GLfloat)Viewports[i].height;
   }
   else
   {
      Viewports[i].aspect = 1.0f;
   }

   // Set GUI frame dimensions.
   guiFrame.setDimensions(Viewports[CONTROLS_VIEWPORT].width,
                          Viewports[CONTROLS_VIEWPORT].height);
   guiFrame.forceUpdate(true);
}


// Reshape.
void reshape(int width, int height)
{
   // Hack to make sure window is what it is reported to be...
   static bool init = true;

   if (init)
   {
      init = false;
      glutReshapeWindow(width, height);
   }

   WindowWidth = width;
   if (height == 0)
   {
      height = 1;
   }
   WindowHeight = height;
   GUIwidth     = (int)((float)WindowWidth / 3.5f);
   configureViewports();
}


// Keyboard input.
#define DELETE_KEY       127
#define RETURN_KEY       13
#define BACKSPACE_KEY    8
void
keyboard(unsigned char key, int x, int y)
{
   // Exit help?
   if (Mode == HELP_MODE)
   {
      Mode = ORGANISM_MODE;
      return;
   }

   // Check for GUI key events.
   if (controlPanel && (controlPanel->getCurrentPanelIndex() == FILE_CONTROL))
   {
      guiFrame.checkKeyboardEvents(KeyEvent(key), KE_PRESSED);
   }
   else
   {
      // Check other key events.
      switch (key)
      {
      // Toggle background.
      case 'b':
         Background = !Background;
         break;

      // Dump.
      case 'd':
         break;

      // Toggle wireframe mode.
      case 'u':
         WireView = !WireView;
         break;

      // Select vessel appearance.
      case 'v':
         switch (VesselAppearance)
         {
         case TRANSPARENT_VESSEL:
            VesselAppearance = WIRE_VESSEL;
            break;

         case WIRE_VESSEL:
            VesselAppearance = INVISIBLE_VESSEL;
            break;

         case INVISIBLE_VESSEL:
            VesselAppearance = TRANSPARENT_VESSEL;
            break;
         }
         break;

      // Full screen.
      case 'w':
         glutFullScreen();
         break;

      // User help.
      case 'h':
         Mode = HELP_MODE;
         break;

      // Quit.
      case 'q':
         termMorphozoic();
         exit(0);
      }
   }

   // Re-display.
   glutPostRedisplay();
}


// Special keyboard input.
void
specialKeyboard(int key, int x, int y)
{
   if (Mode == ORGANISM_MODE)
   {
      if ((CurrentCellID != -1) &&
          (controlPanel->getCurrentPanelIndex() == EDIT_CONTROL) &&
          (strcmp(moveOp->getSelectedItem(), "camera") != 0))
      {
         Cell   *cell = organism->getCell(CurrentCellID);
         string op(moveOp->getSelectedItem());
         Vector delta, position;
         switch (key)
         {
         case GLUT_KEY_UP:
            delta.y = MOVEMENT_DELTA * frameRate.speedFactor;
            break;

         case GLUT_KEY_DOWN:
            delta.y = -MOVEMENT_DELTA * frameRate.speedFactor;
            break;

         case GLUT_KEY_RIGHT:
            delta.x = MOVEMENT_DELTA * frameRate.speedFactor;
            break;

         case GLUT_KEY_LEFT:
            delta.x = -MOVEMENT_DELTA * frameRate.speedFactor;
            break;

         case GLUT_KEY_PAGE_DOWN:
            delta.z = MOVEMENT_DELTA * frameRate.speedFactor;
            break;

         case GLUT_KEY_PAGE_UP:
            delta.z = -MOVEMENT_DELTA * frameRate.speedFactor;
            break;
         }
         position = cell->getPosition() + delta;
         cell->setPosition(position);
      }
      else
      {
         switch (key)
         {
         case GLUT_KEY_UP:
            ViewPosition[1] += MOVEMENT_DELTA * frameRate.speedFactor;
            break;

         case GLUT_KEY_DOWN:
            ViewPosition[1] -= MOVEMENT_DELTA * frameRate.speedFactor;
            break;

         case GLUT_KEY_RIGHT:
            ViewPosition[0] += MOVEMENT_DELTA * frameRate.speedFactor;
            break;

         case GLUT_KEY_LEFT:
            ViewPosition[0] -= MOVEMENT_DELTA * frameRate.speedFactor;
            break;

         case GLUT_KEY_PAGE_DOWN:
            ViewPosition[2] -= MOVEMENT_DELTA * frameRate.speedFactor;
            break;

         case GLUT_KEY_PAGE_UP:
            ViewPosition[2] += MOVEMENT_DELTA * frameRate.speedFactor;
            break;
         }
      }
   }

   // Re-display.
   glutPostRedisplay();
}


// Idle function.
void idle()
{
   TIME t;
   bool run;

   // Exit help if not running.
   if ((Mode == HELP_MODE) &&
       (controlPanel->getCurrentPanelIndex() != RUN_CONTROL))
   {
      Mode = ORGANISM_MODE;
      return;
   }

   // Run organism.
   if ((Mode == ORGANISM_MODE) && (Step || (Delay < MAX_DELAY)))
   {
      if ((Cycles >= 0) && (CycleCounter >= Cycles))
      {
         termMorphozoic();
         exit(0);
      }
      run = true;
      t   = gettime();
      if (Step)
      {
         LastTime = t;
      }
      else
      {
         if ((float)(t - LastTime) >= Delay)
         {
            LastTime = t;
         }
         else
         {
            run = false;
         }
      }
      if (run)
      {
         organism->update();
         if (Cycles >= 0)
         {
            CycleCounter++;
         }
      }
      if (Step)
      {
         Delay = MAX_DELAY;
         delaySlider->setProgress(1.0f);
         Step = false;
      }
   }

   // Set velocity sliders.
   if ((Mode == ORGANISM_MODE) && (CurrentCellID != -1) &&
       (controlPanel->getCurrentPanelIndex() == EDIT_CONTROL))
   {
      Cell  *cell = organism->getCell(CurrentCellID);
      float f;
      char  buf[50];
      f = cell->velocity.x / organism->parameters->MAX_SPEED;
      if (f > 1.0f)
      {
         f = 1.0f;
      }
      if (f < -1.0f)
      {
         f = -1.0f;
      }
      f = (f + 1.0f) / 2.0f;
      xSlider->setProgress(f);
      sprintf(buf, "%.2f", cell->velocity.x);
      xSlider->setLabelString(buf);
      f = cell->velocity.y / organism->parameters->MAX_SPEED;
      if (f > 1.0f)
      {
         f = 1.0f;
      }
      if (f < -1.0f)
      {
         f = -1.0f;
      }
      f = (f + 1.0f) / 2.0f;
      ySlider->setProgress(f);
      sprintf(buf, "%.2f", cell->velocity.y);
      ySlider->setLabelString(buf);
      f = cell->velocity.z / organism->parameters->MAX_SPEED;
      if (f > 1.0f)
      {
         f = 1.0f;
      }
      if (f < -1.0f)
      {
         f = -1.0f;
      }
      f = (f + 1.0f) / 2.0f;
      zSlider->setProgress(f);
      sprintf(buf, "%.2f", cell->velocity.z);
      zSlider->setLabelString(buf);
   }

   // Re-display.
   glutPostRedisplay();
}


// Mouse callbacks.
void mouseClicked(int button, int state, int x, int y)
{
   // Save click info.
   lastButton = button;
   cursorX    = x;
   cursorY    = y;
   if (button != GLUT_LEFT_BUTTON)
   {
      return;
   }

   // Selecting an atom?
   if ((state == GLUT_DOWN) && (Mode == ORGANISM_MODE))
   {
      renderMode = GL_SELECT;
   }

   // Adjust for GUI viewport.
   x -= Viewports[CONTROLS_VIEWPORT].x;
   y -= (WindowHeight - Viewports[CONTROLS_VIEWPORT].height);
   MouseEvent event = MouseEvent(MB_BUTTON1, x, y, guiFrame.getHeight() - y);
   guiFrame.checkMouseEvents(event, (state == GLUT_DOWN) ? ME_CLICKED : ME_RELEASED);
}


void mouseDragged(int x, int y)
{
   x -= Viewports[CONTROLS_VIEWPORT].x;
   y -= (WindowHeight - Viewports[CONTROLS_VIEWPORT].height);
   MouseEvent event = MouseEvent(MB_UNKNOWN_BUTTON, x, y, guiFrame.getHeight() - y);
   guiFrame.checkMouseEvents(event, ME_DRAGGED);
}


void mouseMoved(int x, int y)
{
   x -= Viewports[CONTROLS_VIEWPORT].x;
   y -= (WindowHeight - Viewports[CONTROLS_VIEWPORT].height);
   MouseEvent event = MouseEvent(MB_UNKNOWN_BUTTON, x, y, guiFrame.getHeight() - y);
   guiFrame.checkMouseEvents(event, ME_MOVED);
}


// GUI event handler.
void EventsHandler::actionPerformed(GUIEvent& evt)
{
   const string& callbackString   = evt.getCallbackString();
   GUIRectangle  *sourceRectangle = evt.getEventSource(),
   *parent        = sourceRectangle ? sourceRectangle->getParent() : NULL;
   int widgetType = sourceRectangle->getWidgetType();

   if (widgetType == WT_SLIDER)
   {
      GUISlider *slider = (GUISlider *)sourceRectangle;
      char      buf[50];

      // Delay slider?
      if (callbackString == "delay")
      {
         Delay = slider->getProgress() * MAX_DELAY;
         if (Delay < MAX_DELAY)
         {
            sprintf(buf, "Delay: %.2f (secs)", Delay);
         }
         else
         {
            sprintf(buf, "Delay: STOP");
         }
         slider->setLabelString(buf);
         LastTime = gettime();
         return;
      }

      // X velocity slider?
      bool setVelocity = false;
      if (callbackString == "xspeed")
      {
         sprintf(buf, "%.2f", (slider->getProgress() - 0.5f) *
                 2.0f * organism->parameters->MAX_SPEED);
         slider->setLabelString(buf);
         setVelocity = true;
      }

      // Y velocity slider?
      if (callbackString == "yspeed")
      {
         sprintf(buf, "%.2f", (slider->getProgress() - 0.5f) *
                 2.0f * organism->parameters->MAX_SPEED);
         slider->setLabelString(buf);
         setVelocity = true;
      }

      // Z velocity slider?
      if (callbackString == "zspeed")
      {
         sprintf(buf, "%.2f", (slider->getProgress() - 0.5f) *
                 2.0f * organism->parameters->MAX_SPEED);
         slider->setLabelString(buf);
         setVelocity = true;
      }

      if (setVelocity && (CurrentCellID != -1))
      {
         Vector velocity;
         velocity.x = (xSlider->getProgress() - 0.5f) *
                      2.0f * organism->parameters->MAX_SPEED;
         velocity.y = (ySlider->getProgress() - 0.5f) *
                      2.0f * organism->parameters->MAX_SPEED;
         velocity.z = (zSlider->getProgress() - 0.5f) *
                      2.0f * organism->parameters->MAX_SPEED;
         Cell *cell = organism->getCell(CurrentCellID);
         cell->setVelocity(velocity);
      }
      return;
   }

   if (widgetType == WT_BUTTON)
   {
      GUIButton *button = (GUIButton *)sourceRectangle;
      if (callbackString == "step")
      {
         if (button->isClicked())
         {
            Step = true;
         }
         return;
      }

      if (callbackString == "help")
      {
         if (button->isClicked())
         {
            if (Mode == ORGANISM_MODE)
            {
               Mode = HELP_MODE;
            }
            else
            {
               Mode = ORGANISM_MODE;
            }
         }
         return;
      }

      if (callbackString == "createCell")
      {
         if (button->isClicked())
         {
            Cell *cell = organism->createCell();
         }
         return;
      }

      if (callbackString == "deleteCurrent")
      {
         if (button->isClicked())
         {
            string op(deleteOp->getSelectedItem());
            if (CurrentCellID != -1)
            {
               Cell *cell = organism->getCell(CurrentCellID);
               organism->removeCell(CurrentCellID);
               CurrentCellID = -1;
            }
            if (op == "organism")
            {
               // Delete organism.
               while (organism->cells.size() > 0)
               {
                  organism->removeCell(organism->cells[0]->getID());
               }
               CurrentCellID = -1;
            }
         }
         return;
      }

      if (callbackString == "fileExecute")
      {
         if (button->isClicked())
         {
            FILE   *fp;
            string op(fileOp->getSelectedItem());
            string name = fileNameText->getLabelString();
            if (name != "")
            {
               if (op == "import")
               {
                  if ((fp = FOPEN_READ((char *)name.c_str())) != NULL)
                  {
                     organism->import(fp);
                     fclose(fp);
                  }
               }
               else if (op == "load")
               {
                  if ((fp = FOPEN_READ((char *)name.c_str())) != NULL)
                  {
                     organism->load(fp);
                     fclose(fp);
                  }
               }
               else
               {
                  if ((fp = FOPEN_WRITE((char *)name.c_str())) != NULL)
                  {
                     organism->save(fp);
                     fclose(fp);
                  }
               }
            }
         }
         return;
      }

      if (callbackString == "quit")
      {
         if (button->isClicked())
         {
            termMorphozoic();
            exit(0);
         }
         return;
      }
   }
}


// Picking.
void startPicking()
{
   GLint viewport[4];

   glSelectBuffer(BUFSIZE, selectBuf);
   glGetIntegerv(GL_VIEWPORT, viewport);
   glRenderMode(GL_SELECT);
   glInitNames();
   glMatrixMode(GL_PROJECTION);
   glPushMatrix();
   glLoadIdentity();
   gluPickMatrix((GLdouble)cursorX, (GLdouble)(WindowHeight - cursorY), 5.0, 5.0, viewport);
   gluPerspective(FRUSTUM_ANGLE, Viewports[ORGANISM_VIEWPORT].aspect,
                  FRUSTUM_NEAR, FRUSTUM_FAR);
   glMatrixMode(GL_MODELVIEW);
}


void processHits(GLint hits, GLuint buffer[], int sw)
{
   GLint  i, numberOfNames;
   GLuint names, *ptr, minZ, *ptrNames;
   int    id;

   ptr           = (GLuint *)buffer;
   minZ          = 0xffffffff;
   numberOfNames = 0;
   for (i = 0; i < hits; i++)
   {
      names = *ptr;
      ptr++;
      if (*ptr < minZ)
      {
         numberOfNames = names;
         minZ          = *ptr;
         ptrNames      = ptr + 2;
      }

      ptr += names + 2;
   }
   if (numberOfNames > 0)
   {
      ptr = ptrNames;
      i   = *ptr;
      if (i >= 0)
      {
         id = i;
         if (CurrentCellID != id)
         {
            // Set new current.
            CurrentCellID = id;
         }
         else
         {
            CurrentCellID = -1;
         }
      }
      else
      {
         CurrentCellID = -1;
      }
   }
   else
   {
      if ((cursorX <= Viewports[ORGANISM_VIEWPORT].width) &&
          (cursorY <= Viewports[ORGANISM_VIEWPORT].height))
      {
         CurrentCellID = -1;
      }
   }
}


void stopPicking()
{
   glMatrixMode(GL_PROJECTION);
   glPopMatrix();
   glMatrixMode(GL_MODELVIEW);
   glFlush();
   pickHits = glRenderMode(GL_RENDER);
   processHits(pickHits, selectBuf, 0);
   renderMode = GL_RENDER;
}


// Help for controls.
void helpInfo()
{
   int i, v;

   v = 30;
   for (i = 0; ControlHelp[i] != NULL; i++)
   {
      draw2Dstring(5, v, FONT, ControlHelp[i]);
      v += LINE_SPACE;
   }
   v += LINE_SPACE;
   draw2Dstring(5, v, FONT, (char *)"Press any key to continue...");
}


// Print string on screen at specified location.
void draw2Dstring(GLfloat x, GLfloat y, void *font, char *string)
{
   char *c;

   glRasterPos2f(x, y);
   for (c = string; *c != '\0'; c++)
   {
      glutBitmapCharacter(font, *c);
   }
}


// GUI 2D mode.
void enter2DMode(GLint winWidth, GLint winHeight)
{
   Tuple4i viewport;

   if ((winWidth <= 0) || (winHeight <= 0))
   {
      glGetIntegerv(GL_VIEWPORT, viewport);
      winWidth  = viewport.z;
      winHeight = viewport.w;
   }

   glMatrixMode(GL_MODELVIEW);
   glPushMatrix();
   glLoadIdentity();
   glMatrixMode(GL_PROJECTION);
   glPushMatrix();
   glLoadIdentity();
   gluOrtho2D(0, winWidth, winHeight, 0);
   glDisable(GL_DEPTH_TEST);
   glDisable(GL_LIGHTING);
}


void exit2DMode()
{
   glMatrixMode(GL_PROJECTION);
   glPopMatrix();
   glMatrixMode(GL_MODELVIEW);
   glPopMatrix();
   glEnable(GL_DEPTH_TEST);
   glEnable(GL_LIGHTING);
}


// Initialize.
void initMorphozoic()
{
   int    i;
   FILE   *fp;
   Vector position;

   // Initialize graphics (even if turned off).
   glutInitWindowSize(WindowWidth, WindowHeight);
   glutInitDisplayMode(GLUT_RGBA | GLUT_DOUBLE | GLUT_DEPTH | GLUT_MULTISAMPLE);
   MainWindow = glutCreateWindow("Morphozoic");
   glutDisplayFunc(display);
   glutReshapeFunc(reshape);
   glutKeyboardFunc(keyboard);
   glutSpecialFunc(specialKeyboard);
   glutIdleFunc(idle);
   glutMouseFunc(mouseClicked);
   glutMotionFunc(mouseDragged);
   glutPassiveMotionFunc(mouseMoved);
   glEnable(GL_DEPTH_TEST);
   glShadeModel(GL_SMOOTH);
   glEnable(GL_CULL_FACE);
   glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

   // Load?
   if (LoadFile != NULL)
   {
      if ((fp = FOPEN_READ((char *)LoadFile)) == NULL)
      {
         fprintf(stderr, "Cannot load from file %s\n", LoadFile);
         exit(1);
      }
      FREAD_LONG(&RandomSeed, fp);
      Randomizer = new Random(RandomSeed);
      assert(Randomizer != NULL);
      Randomizer->RAND_LOAD(fp);
      FREAD_INT(&WindowWidth, fp);
      FREAD_INT(&WindowHeight, fp);
      GUIwidth = (int)((float)WindowWidth / 3.5f);
      for (i = 0; i < 3; i++)
      {
         FREAD_FLOAT(&ViewPosition[i], fp);
      }
      FREAD_INT(&i, fp);
      Mode = (MODE)i;
      FREAD_BOOL(&WireView, fp);
#ifdef THREADS
      organism = new Organism(DEFAULT_VESSEL_RADIUS, RandomSeed, NumThreads);
#else
      organism = new Organism(DEFAULT_VESSEL_RADIUS, RandomSeed);
#endif
      assert(organism != NULL);
      organism->load(fp);
      fclose(fp);
   }
   else
   {
      if (RandomSeed == INVALID_RANDOM)
      {
         RandomSeed = (RANDOM)time(NULL);
         Randomizer = new Random(RandomSeed);
         assert(Randomizer != NULL);
      }
#ifdef THREADS
      organism = new Organism(VesselRadius, Randomizer->RAND(), NumThreads);
#else
      organism = new Organism(VesselRadius, Randomizer->RAND());
#endif
      assert(organism != NULL);
      organism->init(NumCells);
   }

   if (Graphics)
   {
      // Initialize camera.
      glMatrixMode(GL_MODELVIEW);
      glLoadIdentity();
      ViewPosition[2] = VesselRadius * 2.0f;
      camera.setPosition(ViewPosition);
      camera.setYaw(180.0f);

      // Light.
      glEnable(GL_LIGHTING);
      glEnable(GL_LIGHT0);
      glLightfv(GL_LIGHT0, GL_AMBIENT, WhiteLight);

      // Configure viewports.
      configureViewports();

      // Initialize GUI.
      GLeeInit();
      char *path = getResourcePath((char *)"images/");
      assert(path != NULL);
      if (path == NULL)
      {
         fprintf(stderr, "Cannot find images\n");
         exit(1);
      }
      MediaPathManager::registerPath(path);
      free(path);
      path = getResourcePath((char *)"GUI/");
      assert(path != NULL);
      if (path == NULL)
      {
         fprintf(stderr, "Cannot find GUI files\n");
         exit(1);
      }
      MediaPathManager::registerPath(path);
      free(path);
      if (!guiFrame.GUIPanel::loadXMLSettings("GUILayout.xml"))
      {
         fprintf(stderr, "Cannot load GUILayout.xml\n");
         exit(1);
      }
      guiFrame.setGUIEventListener(&handler);
      controlPanel = (GUITabbedPanel *)guiFrame.getWidgetByCallbackString("tabs");
      controlPanel->getTabButton(RUN_CONTROL)->setAlphaFadeScale(500.0f);
      controlPanel->getTabButton(EDIT_CONTROL)->setAlphaFadeScale(500.0f);
      controlPanel->getTabButton(FILE_CONTROL)->setAlphaFadeScale(500.0f);
      delaySlider = (GUISlider *)guiFrame.getWidgetByCallbackString("delay");
      delaySlider->setAlphaFadeScale(500.0f);
      delaySlider->setProgress(1.0f);
      delaySlider->setLabelString("Delay: STOP");
      statusText = (GUILabel *)guiFrame.getWidgetByCallbackString("statusText");
      cyclePanel = (GUIPanel *)guiFrame.getWidgetByCallbackString("Cycles");
      deleteOp   = (GUIComboBox *)guiFrame.getWidgetByCallbackString("deleteOp");
      moveOp     = (GUIComboBox *)guiFrame.getWidgetByCallbackString("moveOp");
      xSlider    = (GUISlider *)guiFrame.getWidgetByCallbackString("xspeed");
      xSlider->setAlphaFadeScale(500.0f);
      xSlider->setProgress(0.5f);
      xSlider->setLabelString("0.00");
      ySlider = (GUISlider *)guiFrame.getWidgetByCallbackString("yspeed");
      ySlider->setAlphaFadeScale(500.0f);
      ySlider->setProgress(0.5f);
      ySlider->setLabelString("0.00");
      zSlider = (GUISlider *)guiFrame.getWidgetByCallbackString("zspeed");
      zSlider->setAlphaFadeScale(500.0f);
      zSlider->setProgress(0.5f);
      zSlider->setLabelString("0.00");
      fileNameText = (GUITextBox *)guiFrame.getWidgetByCallbackString("fileName");
      fileOp       = (GUIComboBox *)guiFrame.getWidgetByCallbackString("fileOp");
      cycleDisplay = (GUILabel *)guiFrame.getWidgetByCallbackString("cycleCounter");
      fpsDisplay   = (GUILabel *)guiFrame.getWidgetByCallbackString("fpsCounter");
   }
}


// Run.
void runMorphozoic()
{
   if (Graphics)
   {
      // Main loop.
      frameRate.reset();
      LastTime = gettime();
      glutMainLoop();
   }
   else
   {
      for ( ; CycleCounter < Cycles; CycleCounter++)
      {
         organism->update();
      }
   }
}


// Terminate.
void termMorphozoic()
{
   int  i;
   FILE *fp;

   // Save?
   if (SaveFile != NULL)
   {
      if ((fp = FOPEN_WRITE((char *)SaveFile)) == NULL)
      {
         fprintf(stderr, "Cannot save to file %s\n", SaveFile);
         exit(1);
      }
      FWRITE_LONG(&RandomSeed, fp);
      Randomizer->RAND_SAVE(fp);
      FWRITE_INT(&WindowWidth, fp);
      FWRITE_INT(&WindowHeight, fp);
      for (i = 0; i < 3; i++)
      {
         FWRITE_FLOAT(&ViewPosition[i], fp);
      }
      i = (int)Mode;
      FWRITE_INT(&i, fp);
      FWRITE_BOOL(&WireView, fp);
      organism->save(fp);
      fclose(fp);
   }

   // Release storage.
   delete organism;
   organism = NULL;
   delete Randomizer;
   Randomizer = NULL;
}


#ifdef MORPHOZOIC_MAIN

#ifdef WIN32
#ifdef _DEBUG
// For Windows memory checking, set CHECK_MEMORY = 1
#define CHECK_MEMORY         0
#if (CHECK_MEMORY == 1)
#define MEMORY_CHECK_FILE    "memory.txt"
#include <crtdbg.h>
#endif
#endif
#endif

// Main.
int main(int argc, char *argv[])
{
#if (CHECK_MEMORY == 1)
   {
#endif
   int i;
   bool gotNumCells, gotVesselRadius;
#ifdef THREADS
   bool gotNumThreads;
#endif

#ifdef WIN32
   // Direct stdio to parent console.
   if (AttachConsole(ATTACH_PARENT_PROCESS))
   {
      freopen("CONOUT$", "w", stdout);
      freopen("CONOUT$", "w", stderr);
      freopen("CONIN$", "r", stdin);
   }
#endif

   // Process glut args.
   glutInit(&argc, argv);

   gotNumCells = gotVesselRadius = false;
#ifdef THREADS
   gotNumThreads = false;
#endif
   for (i = 1; i < argc; i++)
   {
      if (strcmp(argv[i], "-cycles") == 0)
      {
         i++;
         if (i >= argc)
         {
            printUsage();
            exit(1);
         }
         Cycles = atoi(argv[i]);
         if (Cycles < 0)
         {
            printUsage();
            exit(1);
         }
         continue;
      }

      if (strcmp(argv[i], "-numCells") == 0)
      {
         i++;
         if (i >= argc)
         {
            printUsage();
            exit(1);
         }
         if ((NumCells = atoi(argv[i])) < 0)
         {
            printUsage();
            exit(1);
         }
         gotNumCells = true;
         continue;
      }

#ifdef THREADS
      if (strcmp(argv[i], "-numThreads") == 0)
      {
         i++;
         if (i >= argc)
         {
            printUsage();
            exit(1);
         }
         if ((NumThreads = atoi(argv[i])) < 1)
         {
            printUsage();
            exit(1);
         }
         gotNumThreads = true;
         continue;
      }
#endif

      if (strcmp(argv[i], "-vesselRadius") == 0)
      {
         i++;
         if (i >= argc)
         {
            printUsage();
            exit(1);
         }
         if ((VesselRadius = atof(argv[i])) < 0.0f)
         {
            printUsage();
            exit(1);
         }
         gotVesselRadius = true;
         continue;
      }

      if (strcmp(argv[i], "-randomSeed") == 0)
      {
         i++;
         if (i >= argc)
         {
            printUsage();
            exit(1);
         }
         RandomSeed = (RANDOM)atoi(argv[i]);
         if ((Randomizer != NULL) || (RandomSeed == INVALID_RANDOM))
         {
            printUsage();
            exit(1);
         }
         Randomizer = new Random(RandomSeed);
         assert(Randomizer != NULL);
         continue;
      }

      if (strcmp(argv[i], "-load") == 0)
      {
         i++;
         if (i >= argc)
         {
            printUsage();
            exit(1);
         }
         LoadFile = argv[i];
         continue;
      }

      if (strcmp(argv[i], "-save") == 0)
      {
         i++;
         if (i >= argc)
         {
            printUsage();
            exit(1);
         }
         SaveFile = argv[i];
         continue;
      }

      if (strcmp(argv[i], "-noGraphics") == 0)
      {
         Graphics = false;
         continue;
      }

      printUsage();
      exit(1);
   }

   if (((RandomSeed != INVALID_RANDOM) ||
        gotNumCells || gotVesselRadius) &&
       (LoadFile != NULL))
   {
      fprintf(stderr, "Properties loaded from file\n");
      printUsage();
      exit(1);
   }

   // Initialize.
   initMorphozoic();

   // Run.
   runMorphozoic();

   // Terminate.
   termMorphozoic();

#if (CHECK_MEMORY == 1)
}


// Check for memory leaks.
printf("Checking for memory leaks, report in file %s\n",
       MEMORY_CHECK_FILE);
HANDLE hFile = CreateFile(
   MEMORY_CHECK_FILE,
   GENERIC_WRITE,
   FILE_SHARE_WRITE,
   NULL,
   OPEN_ALWAYS,
   0,
   NULL
   );
if (hFile == INVALID_HANDLE_VALUE)
{
   fprintf(stderr, "Cannot open memory check file %s",
           MEMORY_CHECK_FILE);
   exit(1);
}

_CrtSetReportMode(_CRT_WARN, _CRTDBG_MODE_FILE);
_CrtSetReportFile(_CRT_WARN, hFile);
_CrtSetReportMode(_CRT_ERROR, _CRTDBG_MODE_FILE);
_CrtSetReportFile(_CRT_ERROR, hFile);
if (!_CrtDumpMemoryLeaks())
{
   printf("No memory leaks\n");
}

else
{
   printf("Memory leaks found\n");
}

CloseHandle(hFile);
#endif

   return(0);
}
#endif
