library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;

library work;
  use work.functions.all;
  use work.psl.all;
  use work.cu_package.all;
  use work.dma_package.all;

entity cu is
  port (
    i                       : in  cu_in;
    o                       : out cu_out
  );
end entity cu;

architecture logic of cu is

  signal q, r               : cu_int;

begin

  comb : process(all)
    variable v              : cu_int;
  begin

----------------------------------------------------------------------------------------------------------------------- default assignments

    v                       := r;
    v.o.read.valid          := '0';
    v.o.write.request.valid := '0';
    v.o.write.data.valid    := '0';

----------------------------------------------------------------------------------------------------------------------- state machine

    case r.state is
      when idle =>
        if i.start then
          v.state           := read_first_from_top;
          v.caller_state    := idle;
          v.inner_counter   := '0';
          v.wed             := i.wed;
          v.o.done          := '0';
        end if;

      when read_first_from_top =>
        read_bytes(v.o.read, v.wed.source, 2*v.wed.imagewidth);
        write_bytes(v.o.write, v.wed.dest_temp, v.wed.imagewidth);
        v.in_counter := 2;
        v.state := first_preprocess_1_from_top;
	
      when first_preprocess_1_from_top =>
        if i.read.valid then
          v.in_buffer.row_0 := (others => '0');
          v.in_buffer.row_1(v.wed.imagewidth downto 1) := u(i.read.data(v.wed.imagewidth-1 downto 0));
          v.in_buffer.row_2(v.wed.imagewidth downto 1) := u(i.read.data(2*v.wed.imagewidth-1 downto v.wed.imagewidth));
          read_bytes(v.o.read, v.wed.dest_temp, 2*v.wed.imagewidth);
          v.temp_counter := 2;
          v.state := first_preprocess_2_from_top;
        end if;

      when first_preprocess_2_from_top =>
        if i.read.valid then
          v.temp_buffer.row_0 := (others => x"FFFFFFFF");
          v.temp_buffer.row_1(v.wed.imagewidth downto 1) := u(i.read.data(v.wed.imagewidth-1 downto 0));
          v.temp_buffer.row_2(v.wed.imagewidth downto 1) := u(i.read.data(2*v.wed.imagewidth-1 downto v.wed.imagewidth));
          if v.caller_state = idle or v.caller_state = fourth_process then 
            v.state := first_process;
          elsif v.caller_state = second_process then
            v.state := third_process;
          end if;
          v.caller_state := first_preprocess_2_from_top;
            
        end if;

      when read_intermediate_from_top =>
        read_bytes(v.o.read, v.wed.source + v.in_counter * v.wed.imagewidth, v.wed.imagewidth);
        write_bytes(v.o.write, v.wed.dest_temp + (v.temp_counter -1) * v.wed.imagewidth, v.wed.imagewidth);
        v.in_counter := v.in_counter + 1; 
        v.state := intermediate_preprocess_1_from_top;
  
      when intermediate_preprocess_1_from_top =>
        if i.read.valid then
          v.in_buffer.row_2(v.wed.imagewidth downto 1) := u(i.read.data(v.wed.imagewidth-1 downto 0));
          read_bytes(v.o.read, v.wed.dest_temp + v.temp_counter * v.wed.imagewidth, v.wed.imagewidth);
          v.temp_counter := v.temp_counter + 1;
          v.state := intermediate_preprocess_2_from_top;
        end if;

      when intermediate_preprocess_2_from_top =>
        if i.read.valid then
          v.temp_buffer.row_2(v.wed.imagewidth downto 1) := u(i.read.data(v.wed.imagewidth-1 downto 0));
          if v.caller_state = first_process then
            v.state := first_process;
          elsif v.caller_state = third_process then
            v.state := third_process;
          end if;
          v.caller_state = intermediate_preprocess_2_from_top;

        end if; 

      when read_last_from_top =>
        write_bytes(v.o.write, v.wed.dest_temp + (v.temp_counter -1) * v.wed.imagewidth, v.wed.imagewidth);
        v.state := last_preprocess_1_from_top;
  
      when last_preprocess_1_from_top =>
        v.in_buffer.row_2(v.wed.imagewidth downto 1) := (others => '0');
        v.state := last_preprocess_2_from_top;

      when last_preprocess_2_from_top =>
        v.temp_buffer.row_2(v.wed.imagewidth downto 1) := (others => x"FFFFFFFF");
        if v.caller_state = first_process then
          v.state = first_process;
        elsif v.caller_state = third_process then
          v.state := third_process;
        end if;
        v.caller_state = last_preprocess_2_from_top;

      when read_first_from_bottom =>
        v.in_counter := v.wed.imageheight - 2;
        v.temp_counter := v.wed.imageheight - 1;
        read_bytes(v.o.read, v.wed.source + v.in_counter * v.wed.imagewidth, 2*v.wed.imagewidth);
        write_bytes(v.o.write, v.wed.dest_temp + v.temp_counter * v.wed.imagewidth, v.wed.imagewidth);
        v.in_counter := v.in_counter - 1;
        v.state := first_preprocess_1_from_bottom;
  
      when first_preprocess_1_from_bottom =>
        if i.read.valid then
          v.in_buffer.row_0(v.wed.imagewidth downto 1) := u(i.read.data(v.wed.imagewidth-1 downto 0));
          v.in_buffer.row_1(v.wed.imagewidth downto 1) := u(i.read.data(2*v.wed.imagewidth-1 downto v.wed.imagewidth));
          v.in_buffer.row_2 := (others => '0');
          read_bytes(v.o.read, v.wed.dest_temp + (v.temp_counter - 1) * v.wed.imagewidth, 2*v.wed.imagewidth);
          v.temp_counter := v.temp_counter - 1;
          v.state := first_preprocess_2_from_bottom;
        end if;

      when first_preprocess_2_from_bottom =>
        if i.read.valid then
          v.temp_buffer.row_0(v.wed.imagewidth downto 1) := u(i.read.data(v.wed.imagewidth-1 downto 0));
          v.temp_buffer.row_1(v.wed.imagewidth downto 1) := u(i.read.data(2*v.wed.imagewidth-1 downto v.wed.imagewidth));
          v.temp_buffer.row_2 := (others => x"FFFFFFFF");
          if v.caller_state = first_process then
            v.state := second_process;
          elsif v.caller_state = third_process then
            v.state := fourth_process;
          end if;
          v.caller_state := first_preprocess_2_from_bottom;    
        end if;

      when read_intermediate_from_bottom =>
        read_bytes(v.o.read, v.wed.source + v.in_counter * v.wed.imagewidth, v.wed.imagewidth);
        write_bytes(v.o.write, v.wed.dest_temp + v.temp_counter * v.wed.imagewidth, v.wed.imagewidth);
        v.in_counter := v.in_counter - 1; 
        v.state := intermediate_preprocess_1_from_bottom;
  
      when intermediate_preprocess_1_from_bottom =>
        if i.read.valid then
          v.in_buffer.row_0(v.wed.imagewidth downto 1) := u(i.read.data(v.wed.imagewidth-1 downto 0));
          read_bytes(v.o.read, v.wed.dest_temp + (v.temp_counter - 1) * v.wed.imagewidth, v.wed.imagewidth);
          v.temp_counter := v.temp_counter - 1;
          v.state := intermediate_preprocess_2_from_bottom;
        end if;

      when intermediate_preprocess_2_from_bottom =>
        if i.read.valid then
          v.temp_buffer.row_0(v.wed.imagewidth downto 1) := u(i.read.data(v.wed.imagewidth-1 downto 0));
          if v.caller_state = second_process then
            v.state := second_process;
          elsif v.caller_state = fourth_process then
            v.state := fourth_process;
          end if;
          v.caller_state := intermediate_preprocess_2_from_bottom;
        end if;

      when read_last_from_bottom =>
        write_bytes(v.o.write, v.wed.dest_temp + v.temp_counter * v.wed.imagewidth, v.wed.imagewidth);
        v.state := last_preprocess_1_from_bottom;
  
      when last_preprocess_1_from_bottom =>
        v.in_buffer.row_0(v.wed.imagewidth downto 1) := (others => '0');
        v.state := last_preprocess_2_from_bottom;

      when last_preprocess_2_from_bottom =>
        v.temp_buffer.row_0(v.wed.imagewidth downto 1) := (others => x"FFFFFFFF");
        if v.caller_state = second_process then
          v.state = second_process;
        elsif v.caller_state = fourth_process then
          v.state = fourth_process;
        end if;
        v.caller_state := last_preprocess_2_from_bottom;

      when first_process =>
      	for iterator in v.wed.imagewidth downto 1 loop
      		v.sub_res(13) := v.in_buffer.row_0(iterator+1);
      		v.sub_res(12) := v.in_buffer.row_1(iterator+1);
      		v.sub_res(11) := v.in_buffer.row_2(iterator+1);
      		v.sub_res(10) := v.in_buffer.row_0(iterator);
      		v.sub_res(9)  := v.in_buffer.row_1(iterator);

      		v.sub_res(8) := v.temp_buffer.row_0(iterator+1);
      		v.sub_res(7) := v.temp_buffer.row_1(iterator+1);
      		v.sub_res(6) := v.temp_buffer.row_2(iterator+1);
      		v.sub_res(5) := v.temp_buffer.row_0(iterator);
      		v.sub_res(4) := v.temp_buffer.row_1(iterator);
      	
      		v.sub_res(3) := abs(v.sub_res(9) - v.sub_res(13));
      		v.sub_res(2) := abs(v.sub_res(9) - v.sub_res(12));
      		v.sub_res(1) := abs(v.sub_res(9) - v.sub_res(11));
      		v.sub_res(0) := abs(v.sub_res(9) - v.sub_res(10));
      	
      		v.temp_buffer.row_1(iterator) :=  v.wed.alpha * min(v.sub_res(4), min(min(v.sub_res(3) + v.sub_res(8) + 1, v.sub_res(2) + v.sub_res(7) + 1), min(v.sub_res(1) + v.sub_res(6) + 1, v.sub_res(0) + v.sub_res(5) + 1)));
      	end loop;

        write_data(v.o.write.data, 0, v.temp_buffer.row_1(v.wed.imagewidth downto v.wed.imagewidth-DMA_DATA_WIDTH));
        write_data(v.o.write.data, 0, v.temp_buffer.row_1(v.wed.imagewidth-DMA_DATA_WIDTH-1 downto 1));
        
        v.in_buffer.row_0 := v.in_buffer.row_1;
        v.in_buffer.row_1 := v.in_buffer.row_2;

        v.temp_buffer.row_0 := v.temp_buffer.row_1;
        v.temp_buffer.row_1 := v.temp_buffer.row_2;

        if v.caller_state = first_preprocess_2_from_top then
          v.state := read_intermediate_from_top;
        elsif v.in_counter = v.wed.imageheight-1 and v.caller_state = first_preprocess_2_from_top then
          v.state := read_last_from_top;
        elsif v.caller_state = last_preprocess_2_from_top then
          v.state := read_first_from_bottom;
        end if;
        v.caller_state = first_process;
          

      when second_process =>
        for iterator in 1 to v.wed.imagewidth loop
          v.sub_res(13) := v.in_buffer.row_0(iterator-1);
          v.sub_res(12) := v.in_buffer.row_1(iterator-1);
          v.sub_res(11) := v.in_buffer.row_2(iterator-1);
          v.sub_res(10) := v.in_buffer.row_0(iterator);
          v.sub_res(9)  := v.in_buffer.row_2(iterator);

          v.sub_res(8) := v.temp_buffer.row_0(iterator-1);
          v.sub_res(7) := v.temp_buffer.row_1(iterator-1);
          v.sub_res(6) := v.temp_buffer.row_2(iterator-1);
          v.sub_res(5) := v.temp_buffer.row_0(iterator);
          v.sub_res(4) := v.temp_buffer.row_2(iterator);
        
          v.sub_res(3) := abs(v.sub_res(9) - v.sub_res(13));
          v.sub_res(2) := abs(v.sub_res(9) - v.sub_res(12));
          v.sub_res(1) := abs(v.sub_res(9) - v.sub_res(11));
          v.sub_res(0) := abs(v.sub_res(9) - v.sub_res(10));
        
          v.temp_buffer.row_1(iterator) :=  v.wed.alpha * min(v.sub_res(4), min(min(v.sub_res(3) + v.sub_res(8) + 1, v.sub_res(2) + v.sub_res(7) + 1), min(v.sub_res(1) + v.sub_res(6) + 1, v.sub_res(0) + v.sub_res(5) + 1)));
        end loop;

        write_data(v.o.write.data, 0, v.temp_buffer.row_1(v.wed.imagewidth downto v.wed.imagewidth-DMA_DATA_WIDTH));
        write_data(v.o.write.data, 0, v.temp_buffer.row_1(v.wed.imagewidth-DMA_DATA_WIDTH-1 downto 1));
        
        v.in_buffer.row_2 := v.in_buffer.row_1;
        v.in_buffer.row_1 := v.in_buffer.row_0;

        v.temp_buffer.row_2 := v.temp_buffer.row_1;
        v.temp_buffer.row_1 := v.temp_buffer.row_0;

        if v.caller_state = first_preprocess_2_from_bottom then
          v.state := read_intermediate_from_bottom;
        elsif v.in_counter = 0 and v.caller_state = intermediate_preprocess_2_from_bottom then
          v.state := read_last_from_bottom;
        elsif v.caller_state = last_preprocess_2_from_bottom then
          v.state := read_first_from_top;
        end if;
        v.caller_state := second_process;

      when third_process =>
        for iterator in 1 to v.wed.imagewidth loop
          v.sub_res(13) := v.in_buffer.row_0(iterator+1);
          v.sub_res(12) := v.in_buffer.row_1(iterator+1);
          v.sub_res(11) := v.in_buffer.row_2(iterator+1);
          v.sub_res(10) := v.in_buffer.row_0(iterator);
          v.sub_res(9)  := v.in_buffer.row_2(iterator);

          v.sub_res(8) := v.temp_buffer.row_0(iterator+1);
          v.sub_res(7) := v.temp_buffer.row_1(iterator+1);
          v.sub_res(6) := v.temp_buffer.row_2(iterator+1);
          v.sub_res(5) := v.temp_buffer.row_0(iterator);
          v.sub_res(4) := v.temp_buffer.row_2(iterator);
        
          v.sub_res(3) := abs(v.sub_res(9) - v.sub_res(13));
          v.sub_res(2) := abs(v.sub_res(9) - v.sub_res(12));
          v.sub_res(1) := abs(v.sub_res(9) - v.sub_res(11));
          v.sub_res(0) := abs(v.sub_res(9) - v.sub_res(10));
        
          v.temp_buffer.row_1(iterator) :=  v.wed.alpha * min(v.sub_res(4), min(min(v.sub_res(3) + v.sub_res(8) + 1, v.sub_res(2) + v.sub_res(7) + 1), min(v.sub_res(1) + v.sub_res(6) + 1, v.sub_res(0) + v.sub_res(5) + 1)));
        end loop;

        write_data(v.o.write.data, 0, v.temp_buffer.row_1(v.wed.imagewidth downto v.wed.imagewidth-DMA_DATA_WIDTH));
        write_data(v.o.write.data, 0, v.temp_buffer.row_1(v.wed.imagewidth-DMA_DATA_WIDTH-1 downto 1));
        
        v.in_buffer.row_0 := v.in_buffer.row_1;
        v.in_buffer.row_1 := v.in_buffer.row_2;

        v.temp_buffer.row_0 := v.temp_buffer.row_1;
        v.temp_buffer.row_1 := v.temp_buffer.row_2;

        if v.caller_state = first_preprocess_2_from_top then
          v.state := read_intermediate_from_top;
        elsif v.in_counter = v.wed.imageheight-1 and v.caller_state = first_preprocess_2_from_top then
          v.state := read_last_from_top;
        elsif v.caller_state = last_preprocess_2_from_top then
          v.state := read_first_from_bottom;
        end if;
        v.caller_state := third_process;

      when fourth_process =>
        for iterator in v.wed.imagewidth downto 1 loop
          v.sub_res(13) := v.in_buffer.row_0(iterator-1);
          v.sub_res(12) := v.in_buffer.row_1(iterator-1);
          v.sub_res(11) := v.in_buffer.row_2(iterator-1);
          v.sub_res(10) := v.in_buffer.row_0(iterator);
          v.sub_res(9)  := v.in_buffer.row_1(iterator);

          v.sub_res(8) := v.temp_buffer.row_0(iterator-1);
          v.sub_res(7) := v.temp_buffer.row_1(iterator-1);
          v.sub_res(6) := v.temp_buffer.row_2(iterator-1);
          v.sub_res(5) := v.temp_buffer.row_0(iterator);
          v.sub_res(4) := v.temp_buffer.row_1(iterator);
        
          v.sub_res(3) := abs(v.sub_res(9) - v.sub_res(13));
          v.sub_res(2) := abs(v.sub_res(9) - v.sub_res(12));
          v.sub_res(1) := abs(v.sub_res(9) - v.sub_res(11));
          v.sub_res(0) := abs(v.sub_res(9) - v.sub_res(10));
        
          v.temp_buffer.row_1(iterator) :=  v.wed.alpha * min(v.sub_res(4), min(min(v.sub_res(3) + v.sub_res(8) + 1, v.sub_res(2) + v.sub_res(7) + 1), min(v.sub_res(1) + v.sub_res(6) + 1, v.sub_res(0) + v.sub_res(5) + 1)));
        end loop;

        write_data(v.o.write.data, 0, v.temp_buffer.row_1(v.wed.imagewidth downto v.wed.imagewidth-DMA_DATA_WIDTH));
        write_data(v.o.write.data, 0, v.temp_buffer.row_1(v.wed.imagewidth-DMA_DATA_WIDTH-1 downto 1));
        
        v.in_buffer.row_2 := v.in_buffer.row_1;
        v.in_buffer.row_1 := v.in_buffer.row_0;

        v.temp_buffer.row_2 := v.temp_buffer.row_1;
        v.temp_buffer.row_1 := v.temp_buffer.row_0;

        if v.caller_state = first_preprocess_2_from_bottom then
          v.state := read_intermediate_from_bottom;
        elsif v.in_counter = 0 and v.caller_state = intermediate_preprocess_2_from_bottom then
          v.state := read_last_from_bottom;
        elsif v.caller_state = last_preprocess_2_from_bottom and v.outer_counter /= 3 then
          v.state := read_first_from_top;
          v.outer_counter := v.outer_counter + 1;
        elsif v.outer_counter = 3 then
          v.state := hold;
        end if;
        v.caller_state := fourth_process;

      when hold =>
      	if i.write.valid then
      		v.state := done;
      	end if;
	
      when done =>
        v.o.done            := '1';
        v.state             := idle;

      when others => null;
    end case;

----------------------------------------------------------------------------------------------------------------------- outputs

    -- drive input registers
    q                       <= v;

    -- outputs
    o                       <= r.o;

  end process;

----------------------------------------------------------------------------------------------------------------------- reset & registers

  reg : process(i.cr)
  begin
    if rising_edge(i.cr.clk) then
      if i.cr.rst then
        cu_reset(r);
      else
        r                   <= q;
      end if;
    end if;
  end process;

end architecture logic;
