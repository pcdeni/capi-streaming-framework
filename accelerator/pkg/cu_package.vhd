library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;

library work;
  use work.psl.all;
  use work.dma_package.all;
  use work.wed.all;

package cu_package is

----------------------------------------------------------------------------------------------------------------------- io

  type cu_in is record
    cr        : cr_in;
    start     : std_logic;
    wed       : wed_type;
    id        : unsigned(DMA_ID_WIDTH - 1 downto 0);
    read      : dma_read_response;
    write     : dma_write_response;
  end record;

  type cu_out is record
    done      : std_logic;
    read      : dma_read_request;
    write     : dma_write_request;
  end record;

----------------------------------------------------------------------------------------------------------------------- internals

  type cu_state is (
    idle,
    hold,
    done,
    read_first_from_top,
    first_preprocess_1_from_top,
    first_preprocess_2_from_top,
    read_intermediate_from_top,
    intermediate_preprocess_1_from_top,
    intermediate_preprocess_2_from_top,
    read_last_from_top,
    last_preprocess_1_from_top,
    last_preprocess_2_from_top,
    read_first_from_bottom,
    first_preprocess_1_from_bottom,
    first_preprocess_2_from_bottom,
    read_intermediate_from_bottom,
    intermediate_preprocess_1_from_bottom,
    intermediate_preprocess_2_from_bottom,
    read_last_from_bottom,
    last_preprocess_1_from_bottom,
    last_preprocess_2_from_bottom,
    first_process,
    second_process,
    third_process,
    fourth_process
  );

  type input_buffer is record	--3 rows of the image
    --			a row		of 8bit grayscale pixel data
    row_0 is array of (1436+1 downto 0) of unsigned(7 downto 0);	-- sorry, hardcoded image width
    row_1 is array of (1436+1 downto 0) of unsigned(7 downto 0);	-- sorry, hardcoded image width
    row_2 is array of (1436+1 downto 0) of unsigned(7 downto 0);	-- sorry, hardcoded image width
  end record;

  type temp_buffer is record	--3 rows of the image
    --			a row		of 32bit intermediate sub result data
    row_0 is array of (1436+1 downto 0) of unsigned(31 downto 0);	-- sorry, hardcoded image width
    row_1 is array of (1436+1 downto 0) of unsigned(31 downto 0);	-- sorry, hardcoded image width
    row_2 is array of (1436+1 downto 0) of unsigned(31 downto 0);	-- sorry, hardcoded image width
  end record;

  intermediate_res is array of (13 downto 0) of unsigned(31 downto 0);

  counter_type : unsigned(9 downto 0);

  type cu_int is record
    in_buffer     : input_buffer;
    temp_buffer   : temp_buffer;
    sub_res       : intermediate_res;
    in_counter    : counter_type;
    temp_counter  : counter_type;
    outer_counter : counter_type;
    state         : cu_state;
    caller_state  : cu_state;
    wed           : wed_type;
    o             : cu_out;
  end record;

  procedure cu_reset (signal r : inout cu_int);

end package cu_package;

package body cu_package is

  procedure cu_reset (signal r : inout cu_int) is
  begin
    r.state   <= idle;
    r.caller_state <= idle;
    r.o.done  <= '0';
    r.in_buffer <= (others => (others => '0', others => '0', others => '0'));
    r.temp_buffer <= (others => (others => x"FFFFFFFF", others => x"FFFFFFFF", others => x"FFFFFFFF"));
    r.sub_res <= (others => (others => '0'));
    r.in_counter <= (others => '0');
    r.temp_counter <= (others => '0');
    r.outer_counter <= (others => '0');
  end procedure cu_reset;

end package body cu_package;
