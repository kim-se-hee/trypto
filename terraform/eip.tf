resource "aws_eip" "sut" {
  domain = "vpc"
  tags   = merge(var.tags, { Name = "trypto-sut-eip" })

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_eip" "loadgen" {
  domain = "vpc"
  tags   = merge(var.tags, { Name = "trypto-loadgen-eip" })

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_eip_association" "sut" {
  instance_id   = aws_instance.sut.id
  allocation_id = aws_eip.sut.id
}

resource "aws_eip_association" "loadgen" {
  instance_id   = aws_instance.loadgen.id
  allocation_id = aws_eip.loadgen.id
}
