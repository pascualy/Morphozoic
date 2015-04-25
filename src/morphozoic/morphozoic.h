/*
 * This software is provided under the terms of the GNU General
 * Public License as published by the Free Software Foundation.
 *
 * Copyright (c) 2015 Tom Portegys, All Rights Reserved.
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.
 */

/*
 * Morphozoic driver.
 */

#ifndef __MORPHOZOIC__
#define __MORPHOZOIC__

#include "organism.hpp"
using namespace morphozoic;

// Organism.
#define DEFAULT_NUM_CELLS 0
extern int NumCells;
extern Organism *organism;

// Vessel.
#define DEFAULT_VESSEL_RADIUS 15.0f
extern float VesselRadius;

#ifdef THREADS
// Threads.
#define DEFAULT_NUM_THREADS 1
extern int NumThreads;
#endif

// Run cycles.
extern int Cycles;
extern int CycleCounter;

// Random numbers.
extern RANDOM RandomSeed;
extern Random *Randomizer;

// Files.
extern char *SaveFile;
extern char *LoadFile;

// Window size.
#define WINDOW_WIDTH 850
#define WINDOW_HEIGHT 600

// Initialize, run, and terminate.
void initMorphozoic();
void runMorphozoic();
void termMorphozoic();

// Graphics mode.
extern bool Graphics;

// Speed control (secs, graphics only): 0.0=full speed, MAX_DELAY=stop
#define MAX_DELAY 1.0f
extern float Delay;

// Step control (graphics only).
extern bool Step;
#endif
