package org.electroncash.util;

class HeaderChunk {
  private int HEADER_SIZE = 80;
  private int base_height;
  private byte[] data;
  private int header_count;
  
  public HeaderChunk(int base_height, byte[] data) { this.base_height = base_height;
    this.data = data;
    header_count = (data.length / HEADER_SIZE);
  }
  
  public int get_count() { return header_count; }
  
  public boolean contains_height(int height)
  {
    return (height >= base_height) && (height < base_height + header_count);
  }
  
  public byte[] get_header_at_height(int height)
  {
    byte[] header_hex = get_header_at_index(height - base_height);
    
    return header_hex;
  }
  
  public byte[] get_header_at_index(int index) {
    int header_offset = index * HEADER_SIZE;
    return Arrays.slice(data, header_offset, header_offset + HEADER_SIZE);
  }
}
