package com.hotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.HotelRequest;
import com.hotel.model.Hotel;
import com.hotel.service.HotelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HotelController.class)
class HotelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HotelService hotelService;

    @Test
    void testGetAllHotels() throws Exception {
        Hotel hotel1 = createHotel(1L, "Hotel 1");
        Hotel hotel2 = createHotel(2L, "Hotel 2");
        List<Hotel> hotels = Arrays.asList(hotel1, hotel2);

        when(hotelService.getAllHotels()).thenReturn(hotels);

        mockMvc.perform(get("/v1/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Hotel 1"))
                .andExpect(jsonPath("$[1].name").value("Hotel 2"));

        verify(hotelService).getAllHotels();
    }

    @Test
    void testGetHotelById() throws Exception {
        Hotel hotel = createHotel(1L, "Test Hotel");

        when(hotelService.getHotelById(1L)).thenReturn(hotel);

        mockMvc.perform(get("/v1/hotels/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotelId").value(1))
                .andExpect(jsonPath("$.name").value("Test Hotel"));

        verify(hotelService).getHotelById(1L);
    }

    @Test
    void testCreateHotel() throws Exception {
        HotelRequest request = new HotelRequest();
        request.setName("New Hotel");
        request.setAddress("123 Main St");
        request.setLocation("Test City");

        Hotel createdHotel = createHotel(1L, "New Hotel");

        when(hotelService.createHotel(any(HotelRequest.class))).thenReturn(createdHotel);

        mockMvc.perform(post("/v1/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hotelId").value(1))
                .andExpect(jsonPath("$.name").value("New Hotel"));

        verify(hotelService).createHotel(any(HotelRequest.class));
    }

    @Test
    void testUpdateHotel() throws Exception {
        HotelRequest request = new HotelRequest();
        request.setName("Updated Hotel");
        request.setAddress("456 Main St");
        request.setLocation("Updated City");

        Hotel updatedHotel = createHotel(1L, "Updated Hotel");

        when(hotelService.updateHotel(anyLong(), any(HotelRequest.class))).thenReturn(updatedHotel);

        mockMvc.perform(put("/v1/hotels/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Hotel"));

        verify(hotelService).updateHotel(eq(1L), any(HotelRequest.class));
    }

    @Test
    void testDeleteHotel() throws Exception {
        doNothing().when(hotelService).deleteHotel(1L);

        mockMvc.perform(delete("/v1/hotels/1"))
                .andExpect(status().isNoContent());

        verify(hotelService).deleteHotel(1L);
    }

    private Hotel createHotel(Long id, String name) {
        Hotel hotel = new Hotel();
        hotel.setHotelId(id);
        hotel.setName(name);
        hotel.setAddress("Test Address");
        hotel.setLocation("Test Location");
        return hotel;
    }
}
