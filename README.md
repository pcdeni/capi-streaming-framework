# capi-streaming-framework

AFU framework for streaming applications with CAPI connected FGPAs.

More info here: [http://slides.com/mbrobbel/capi-streaming-framework](http://slides.com/mbrobbel/capi-streaming-framework)

**Please note the following;**

* **It is recommended to read the [CAPI Users Guide](http://www.nallatech.com/wp-content/uploads/IBM_CAPI_Users_Guide_1-2.pdf) before using this framework.**

* **For now there is very limited instructions and documentation, but this will all be added later. An example project file for the Nallatech P385-A7 card with the Altera Stratix V GX A7 FPGA will also be added later. The current Computing Unit (CU) implements a simple memcpy function.**

* **This framework runs in dedicated mode and was developed to be used with Linux.**

## Overview

This will be added later.

## Organization

* `accelerator`
  * `lib` - VHDL global packages
    * `functions.vhd` - Helper functions
    * `psl.vhd` - PSL constants and interface records
    * `wed.vhd` - WED record and parse procedure
  * `pkg` - VHDL packages
  * `rtl` - VHDL architectures
    * `afu.vhd` - PSL to AFU wrapper
    * `control.vhd` - Framework control
    * `cu.vhd` - Computing Unit - implements the actual AFU functionality
    * `dma.vhd` - Direct Memory Access
    * `fifo.vhd` - First-In-First-Out
    * `frame.vhd` - AFU top level
    * `mmio.vhd` - Memory-Mapped-Input-Output
    * `ram.vhd` - Random-Access-Memory
* `host`
	* `app` - Host application sources
* `sim`
  * `pslse` - [PSL Simulation Engine](https://github.com/ibm-capi/pslse) sources
  * *`pslse.parms`* - PSLSE parameter file
  * *`pslse_server.dat`* - PSLSE server used by the host application to attach
  * *`shim_host.dat`* - Simulation host used by the PSLSE
  * *`vsim.tcl`* - Compilation and simulation script for vsim
  * *`wave.do`* - Wave script for vsim
* *`Makefile`* - Global makefile

## Details

This will be added later.

### AFU wrapper and Frame

### Control

### Memory-Mapped-Input-Output (MMIO)

### Direct Memory Access (DMA)

### Computing Unit (CU)

The Computing Unit (CU) implements the actual function of the AFU.

#### Work-Element-Descriptor (WED)

#### DMA procedures

The `dma_package` defines a number of procedures that can be used to communicate with the DMA. They will be updated soon to match the specifications in the slides.

##### Read procedures

##### Write procedures

## Simulation

The following instructions target ModelSim (vsim).

Starting from release 15.0 of Quartus II, the included [ModelSim-Altera Starter Edition](https://www.altera.com/products/design-software/model---simulation/modelsim-altera-software.html) (free) has mixed-language support, which is required for simulation of this framework with the current PSLSE.

It is assumed that the 32-bit version of vsim is installed in `/opt/altera/15.0/modelsim_ase/` and `/opt/altera/15.0/modelsim_ase/bin` is added to your PATH.

Please note that all listed `make` commands should be executed from the root of this project.

### Initial setup

1. Clone the repository. enter the directory and initialize the submodules
  ```bash
  git clone https://github.com/mbrobbel/capi-streaming-framework.git
  cd capi-streaming-framework
  git submodule update --init
  ```

2. Set your `VPI_USER_H_DIR` environment variable to point to the `include` directory of your simulator e.g.:
  ```bash
  export VPI_USER_H_DIR=/opt/altera/15.0/modelsim_ase/include
  ```

3. Build the [`PSLSE`](https://github.com/ibm-capi/pslse):
  ```bash
  make pslse-build
  ```
  This will build the PSLSE with the DEBUG flag and the AFU driver for a 32-bit simulator.

4. Build the host application for simulation:
  ```bash
  make sim-build
  ```

### Run simulation

1. Start the simulator:
  ```bash
  make vsim-run
  ```

  This will start vsim and execute the `vsim.tcl` script, which will automatically compile the sources.

2. Start simulation:

  Use the following command in the vsim console to start the simulation.
  ```bash
  s
  ```

3. Open a new terminal and start the PSLSE:
  ```bash
  make pslse-run
  ```

4. Open a new terminal and run your host application. This will run your host application from the `sim` directory:
  ```bash
  make sim-run ARGS="<number-of-cachelines-to-copy>"
  ```

5. Wait for your host application to terminate then switch to the PSLSE terminal and kill (`CTRL+C`) the running PSLSE process to inspect the wave.

### Development

During development `vsim` can be kept running.

The `vsim.tcl` script also allows to quickly run the following commands again from the `vsim` console:
* `r` - Recompile the `HDL` source files
* `s` - Start the simulation
* `rs` - Recompile the `HDL` source files and restart the simulation

## FPGA build

This will be added later. A timing issue needs to be resolved first.

# Medical image processing pipeline

## About the pipeline

There are images of breast tissue samples made with microscope, and we want to classify the tissue into fat, stroma and carcinoma.
The algorithm goes through the sample images, traines its neural network and then classify every image. (nutshell version)
For this there is the Discrete Transform On Curved Space which is a calculating a value for every pixel based on a neighborhood (3x3)

a b c

d e f

g h i

The DTOCS algorithm calculates a new value for e based on the neighborhood a,b,d,e,g, it iterates through the image in this pattern from top left to bottom right.
Next iterates from bottom right to top left with the neighborhood c,e,f,h,i.
Step 3 iterates from bottom left to top right with the neigborhood a,d,e,g,h.
Step 4, top right to bottom left, b,c,e,f,i.
Then it repeats these 4 steps 4 times, so we have 16 steps.
The catch is that we have big images, and whenewer we calculated the new value for e we don't need the old value, but the new one.

## Implementation

The algorithm runs in Spark which is ~java, which runs in java virtual machine.
We want to accelerate the DTOCS part of the algorithm with an FPGA on an IBM Power 8 system using CAPI, but how can we pass variables to the accelerator?
We have a cpp function for that, but how do we pass variables from java to cpp?
There is Java Native Interface for that. :)
So whenewer we call the DTOCS in spark it will actually call a cpp function, which then gives the physical memory addresses of the passed variables to the FPGA, it processes the data and writes the results back to a memory region with a predefined starting address.
Simple, right? 

I don't get into the details of the memory organization and how to pass data to the FPGA, MBROBBEL implemented it and explained it, go take a look on the original repo.

## JNI

http://www.ibm.com/developerworks/java/tutorials/j-jni/j-jni.html
