package com.p2.isitout.dto.response;

import com.p2.isitout.client.tmdb.dto.TmdbTvShowDetailsDto;
import com.p2.isitout.dto.UserDto;
import lombok.Data;

import java.util.List;

@Data
public class MyDetailsDto {
    private UserDto userDto;
    private List<TmdbTvShowDetailsDto> showDetails;
}
