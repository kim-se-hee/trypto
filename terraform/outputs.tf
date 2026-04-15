output "sut_public_ip" {
  value = aws_eip.sut.public_ip
}

output "loadgen_public_ip" {
  value = aws_eip.loadgen.public_ip
}

output "sut_private_ip" {
  value = aws_instance.sut.private_ip
}

output "k6_run_command" {
  value = <<-EOT
    ssh -i ~/.ssh/trypto-key-pair.pem ubuntu@${aws_eip.loadgen.public_ip} \
      'until [ -f READY ]; do sleep 5; done; \
       K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT=/home/ubuntu/report.html \
       K6_WEB_DASHBOARD_HOST=0.0.0.0 K6_WEB_DASHBOARD_PORT=5665 \
       k6 run /home/ubuntu/trypto/loadtest/k6/scenarios/match_pending.js \
         --env API_TARGET=http://${aws_instance.sut.private_ip}:8080 \
         --env COLLECTOR_TARGET=http://${aws_instance.sut.private_ip}:8081'
  EOT
}

output "dashboards" {
  value = <<-EOT
    k6:       http://${aws_eip.loadgen.public_ip}:5665
    Grafana:  http://${aws_eip.sut.public_ip}:3000
    RabbitMQ: http://${aws_eip.sut.public_ip}:15672
  EOT
}
