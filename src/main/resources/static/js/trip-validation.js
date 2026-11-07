document.addEventListener("DOMContentLoaded", function () {
    const tripForm = document.querySelector("form");
    if (!tripForm) return;

    tripForm.addEventListener("submit", function (event) {
        let isValid = true;
        const departureTimeInput = document.getElementById("departureTime");
        const ticketPriceInput = document.getElementById("ticketPrice");
        const totalSeatsInput = document.getElementById("totalSeats");

        // Validate thời gian xuất bến phải lớn hơn hiện tại
        if (departureTimeInput && departureTimeInput.value) {
            const selectedDate = new Date(departureTimeInput.value);
            const now = new Date();
            if (selectedDate <= now) {
                alert("Thời gian xuất bến phải ở trong tương lai!");
                departureTimeInput.classList.add("is-invalid");
                isValid = false;
            } else {
                departureTimeInput.classList.remove("is-invalid");
            }
        }

        // Validate giá vé lớn hơn 0
        if (ticketPriceInput && parseFloat(ticketPriceInput.value) <= 0) {
            alert("Giá vé phải lớn hơn 0 VNĐ!");
            ticketPriceInput.classList.add("is-invalid");
            isValid = false;
        } else if (ticketPriceInput) {
            ticketPriceInput.classList.remove("is-invalid");
        }

        // Validate tổng số ghế phải từ 1 trở lên
        if (totalSeatsInput && parseInt(totalSeatsInput.value) <= 0) {
            alert("Tổng số ghế phải tối thiểu là 1 ghế!");
            totalSeatsInput.classList.add("is-invalid");
            isValid = false;
        } else if (totalSeatsInput) {
            totalSeatsInput.classList.remove("is-invalid");
        }

        if (!isValid) {
            event.preventDefault();
        }
    });
});